package ru.konyaev.priem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.konyaev.priem.config.ApplicationConfig;
import ru.konyaev.priem.dto.request.RegisterApplicationRequest;
import ru.konyaev.priem.dto.response.ApplicationResponse;
import ru.konyaev.priem.dto.response.PageResponse;
import ru.konyaev.priem.entity.Application;
import ru.konyaev.priem.entity.DailyLimit;
import ru.konyaev.priem.entity.SlotConfig;
import ru.konyaev.priem.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final BlockedDateRepository blockedDateRepository;
    private final OpenedDateRepository openedDateRepository;
    private final AllowedMonthRepository allowedMonthRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final DailyLimitRepository dailyLimitRepository;
    private final SettingsService settingsService;

    @Transactional
    public Map<String, Object> register(RegisterApplicationRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Тело запроса отсутствует");
        }

        String fio = req.getFio() != null ? req.getFio().trim() : "";
        String phone = req.getPhone() != null ? req.getPhone().trim() : "";
        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        String date = req.getRegistrationDate() != null ? req.getRegistrationDate().trim() : "";
        String time = req.getRegistrationTime() != null ? req.getRegistrationTime().trim() : "";

        if (fio.isBlank() || phone.isBlank() || email.isBlank() || date.isBlank() || time.isBlank()) {
            throw new IllegalArgumentException("Все поля обязательны для заполнения");
        }

        if (fio.length() > 255) {
            throw new IllegalArgumentException("ФИО не может быть длиннее 255 символов");
        }
        if (phone.length() > 20) {
            throw new IllegalArgumentException("Телефон не может быть длиннее 20 символов");
        }
        if (email.length() > 100) {
            throw new IllegalArgumentException("Email не может быть длиннее 100 символов");
        }

        if (!ApplicationConfig.TIME_SLOTS.contains(time)) {
            throw new IllegalArgumentException("Недопустимое время записи");
        }

        LocalDate dateObj;
        try {
            dateObj = LocalDate.parse(date);
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный формат даты (ожидается YYYY-MM-DD)");
        }

        ZonedDateTime now = ZonedDateTime.now(ApplicationConfig.ZONE_MOSCOW);
        LocalDateTime slotLdt;
        try {
            slotLdt = LocalDateTime.parse(date + "T" + time + ":00");
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный формат даты или времени");
        }
        ZonedDateTime slotZdt = slotLdt.atZone(ApplicationConfig.ZONE_MOSCOW);

        int advanceHours = settingsService.getAdvanceHours();
        if (!slotZdt.isAfter(now.plusHours(advanceHours))) {
            throw new IllegalArgumentException("Запись доступна минимум за " + advanceHours + " ч до приёма");
        }

        if (!allowedMonthRepository.existsByYearAndMonth(dateObj.getYear(), dateObj.getMonthValue())) {
            throw new IllegalArgumentException("Запись на этот месяц недоступна");
        }

        if (dateObj.getDayOfWeek() == DayOfWeek.SATURDAY || dateObj.getDayOfWeek() == DayOfWeek.SUNDAY) {
            if (!openedDateRepository.existsByDate(date)) {
                throw new IllegalArgumentException("Запись в выходные дни недоступна");
            }
        }

        if (blockedDateRepository.existsByDate(date)) {
            throw new IllegalArgumentException("Запись на эту дату недоступна");
        }

        SlotConfig slotCfg = slotConfigRepository.findByDateAndTime(date, time).orElse(null);
        if (slotCfg != null && Boolean.TRUE.equals(slotCfg.getIsBlocked())) {
            throw new IllegalArgumentException("Запись на это время недоступна");
        }

        int maxCapacity = (slotCfg != null && slotCfg.getMaxCapacity() != null)
                ? slotCfg.getMaxCapacity() : ApplicationConfig.DEFAULT_MAX_CAPACITY;

        if (applicationRepository.findExistingByDateAndPhoneOrEmail(date, phone, email).isPresent()) {
            throw new IllegalArgumentException("Вы уже записаны на эту дату");
        }

        DailyLimit dailyLimitRow = dailyLimitRepository.findByDate(date).orElse(null);
        if (dailyLimitRow != null && dailyLimitRow.getMaxRegistrations() != null && dailyLimitRow.getMaxRegistrations() > 0) {
            long dailyCount = applicationRepository.countByRegistrationDate(date);
            if (dailyCount >= dailyLimitRow.getMaxRegistrations()) {
                throw new IllegalArgumentException("На этот день достигнут лимит записей");
            }
        }

        long slotsCount = applicationRepository.countByRegistrationDateAndRegistrationTime(date, time);
        if (slotsCount >= maxCapacity) {
            throw new IllegalArgumentException("Извините, на это время мест больше нет");
        }

        Application application = Application.builder()
                .fio(fio)
                .phone(phone)
                .email(email)
                .registrationDate(date)
                .registrationTime(time)
                .status("pending")
                .build();

        Application saved = applicationRepository.save(application);
        log.info("Registered application #{}: {} on {} {}", saved.getId(), fio, date, time);

        return Map.of("message", "Success", "id", saved.getId());
    }

    public PageResponse<ApplicationResponse> getApplications(
            int page, int pageSize, String search, String sortField, String order
    ) {
        int safePage = Math.max(1, Math.min(page, 10000));
        int safePageSize = Math.max(1, Math.min(200, pageSize));

        Specification<Application> spec = Specification.where(null);
        if (StringUtils.hasText(search)) {
            String safeSearch = search.trim();
            if (safeSearch.length() > 200) safeSearch = safeSearch.substring(0, 200);
            String like = "%" + safeSearch.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fio")), like),
                    cb.like(cb.lower(root.get("phone")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("registrationDate")), like),
                    cb.like(cb.lower(root.get("registrationTime")), like)
            ));
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort;
        if ("fio".equalsIgnoreCase(sortField)) {
            sort = Sort.by(direction, "fio");
        } else if ("registration_time".equalsIgnoreCase(sortField)) {
            sort = Sort.by(direction, "registrationTime").and(Sort.by(direction, "registrationDate"));
        } else if ("created_at".equalsIgnoreCase(sortField)) {
            sort = Sort.by(direction, "createdAt");
        } else {
            sort = Sort.by(direction, "registrationDate").and(Sort.by(direction, "registrationTime"));
        }

        Pageable pageable = PageRequest.of(safePage - 1, safePageSize, sort);
        Page<Application> appPage = applicationRepository.findAll(spec, pageable);

        List<ApplicationResponse> items = appPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<ApplicationResponse>builder()
                .items(items)
                .total(appPage.getTotalElements())
                .page(safePage)
                .pageSize(safePageSize)
                .totalPages(Math.max(1, appPage.getTotalPages()))
                .build();
    }

    @Transactional
    public void deleteApplication(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Некорректный ID заявления");
        }
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Запись не найдена"));
        applicationRepository.delete(app);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String newStatus) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Некорректный ID заявления");
        }
        if (newStatus == null || (
                !"pending".equalsIgnoreCase(newStatus.trim()) &&
                !"confirmed".equalsIgnoreCase(newStatus.trim()) &&
                !"rejected".equalsIgnoreCase(newStatus.trim()))) {
            throw new IllegalArgumentException("Недопустимый статус (ожидается pending, confirmed или rejected)");
        }
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Запись не найдена"));
        app.setStatus(newStatus.trim().toLowerCase());
        applicationRepository.save(app);

        return Map.of("id", id, "status", app.getStatus());
    }

    public List<Application> getApplicationsForExport(String filterDate) {
        if (StringUtils.hasText(filterDate)) {
            return applicationRepository.findByRegistrationDateOrderByRegistrationDateAscRegistrationTimeAsc(filterDate.trim());
        }
        return applicationRepository.findAllByOrderByRegistrationDateAscRegistrationTimeAsc();
    }

    private ApplicationResponse mapToResponse(Application a) {
        return ApplicationResponse.builder()
                .id(a.getId())
                .fio(a.getFio())
                .phone(a.getPhone())
                .email(a.getEmail())
                .registrationDate(a.getRegistrationDate())
                .registrationTime(a.getRegistrationTime())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt().atZone(ApplicationConfig.ZONE_MOSCOW).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }
}
