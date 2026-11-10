package ru.konyaev.priem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Сервис бизнес-логики для работы с заявлениями абитуриентов:
 * запись с проверкой всех ограничений, поиск, пагинация, смена статусов и удаление.
 */
@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final BlockedDateRepository blockedDateRepository;
    private final OpenedDateRepository openedDateRepository;
    private final AllowedMonthRepository allowedMonthRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final DailyLimitRepository dailyLimitRepository;
    private final SettingsService settingsService;

    @Autowired
    public ApplicationService(
            ApplicationRepository applicationRepository,
            BlockedDateRepository blockedDateRepository,
            OpenedDateRepository openedDateRepository,
            AllowedMonthRepository allowedMonthRepository,
            SlotConfigRepository slotConfigRepository,
            DailyLimitRepository dailyLimitRepository,
            SettingsService settingsService
    ) {
        this.applicationRepository = applicationRepository;
        this.blockedDateRepository = blockedDateRepository;
        this.openedDateRepository = openedDateRepository;
        this.allowedMonthRepository = allowedMonthRepository;
        this.slotConfigRepository = slotConfigRepository;
        this.dailyLimitRepository = dailyLimitRepository;
        this.settingsService = settingsService;
    }

    /**
     * Регистрация нового заявления абитуриента.
     * Все нарушения правил выбрасывают IllegalArgumentException (возвращается HTTP 400).
     */
    @Transactional
    public Map<String, Object> register(RegisterApplicationRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Тело запроса отсутствует");
        }

        // 1. Очистка пробелов и проверка обязательных полей
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

        // 2. Проверка даты и правила заблаговременной записи (по умолчанию 24 ч)
        LocalDate dateObj;
        try {
            dateObj = LocalDate.parse(date);
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный формат даты (ожидается YYYY-MM-DD)");
        }

        ZonedDateTime now = ZonedDateTime.now(ApplicationConfig.ZONE_MOSCOW);
        if (dateObj.isBefore(now.toLocalDate())) {
            throw new IllegalArgumentException("Выбранная дата уже прошла (" + date + "). Пожалуйста, выберите актуальную дату приёма.");
        }

        LocalDateTime slotLdt;
        try {
            slotLdt = LocalDateTime.parse(date + "T" + time + ":00");
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный формат даты или времени");
        }
        ZonedDateTime slotZdt = slotLdt.atZone(ApplicationConfig.ZONE_MOSCOW);

        int advanceHours = settingsService.getAdvanceHours();
        if (!slotZdt.isAfter(now.plusHours(advanceHours))) {
            throw new IllegalArgumentException("Запись на это время закрыта (требуется оформление минимум за " + advanceHours + " ч до приёма). Пожалуйста, выберите более позднее время или дату.");
        }

        // 3. Проверка разрешённого месяца, выходных и заблокированных дат
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

        // 4. Проверка блокировки конкретного 10-минутного слота
        SlotConfig slotCfg = slotConfigRepository.findByDateAndTime(date, time).orElse(null);
        if (slotCfg != null && Boolean.TRUE.equals(slotCfg.getIsBlocked())) {
            throw new IllegalArgumentException("Запись на это время недоступна");
        }

        int maxCapacity = (slotCfg != null && slotCfg.getMaxCapacity() != null)
                ? slotCfg.getMaxCapacity()
                : ApplicationConfig.DEFAULT_MAX_CAPACITY;

        // 5. Защита от повторной записи одним человеком на одну и ту же дату
        if (applicationRepository.findExistingByDateAndPhoneOrEmail(date, phone, email).isPresent()) {
            throw new IllegalArgumentException("Вы уже записаны на эту дату");
        }

        // 6. Проверка общего дневного лимита записей
        DailyLimit dailyLimitRow = dailyLimitRepository.findByDate(date).orElse(null);
        if (dailyLimitRow != null && dailyLimitRow.getMaxRegistrations() != null && dailyLimitRow.getMaxRegistrations() > 0) {
            long dailyCount = applicationRepository.countByRegistrationDate(date);
            if (dailyCount >= dailyLimitRow.getMaxRegistrations()) {
                throw new IllegalArgumentException("На этот день достигнут лимит записей");
            }
        }

        // 7. Проверка свободных мест в выбранном слоте
        long slotsCount = applicationRepository.countByRegistrationDateAndRegistrationTime(date, time);
        if (slotsCount >= maxCapacity) {
            throw new IllegalArgumentException("Извините, на это время мест больше нет");
        }

        // 8. Создание сущности и сохранение в БД
        Application application = new Application();
        application.setFio(fio);
        application.setPhone(phone);
        application.setEmail(email);
        application.setRegistrationDate(date);
        application.setRegistrationTime(time);
        application.setStatus("pending");

        Application saved = applicationRepository.save(application);
        log.info("Registered application #{}: {} on {} {}", saved.getId(), fio, date, time);

        return Map.of("message", "Success", "id", saved.getId());
    }

    /** Получение списка активных заявлений с поиском, сортировкой и пагинацией. */
    public PageResponse<ApplicationResponse> getApplications(
            int page, int pageSize, String search, String sortField, String order
    ) {
        int safePage = Math.max(1, Math.min(page, 10000));
        int safePageSize = Math.max(1, Math.min(200, pageSize));

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
        Page<Application> appPage;

        if (StringUtils.hasText(search)) {
            String safeSearch = search.trim();
            if (safeSearch.length() > 200) {
                safeSearch = safeSearch.substring(0, 200);
            }
            appPage = applicationRepository.searchApplications(safeSearch, pageable);
        } else {
            appPage = applicationRepository.findAll(pageable);
        }

        List<ApplicationResponse> items = new ArrayList<>();
        for (Application app : appPage.getContent()) {
            items.add(mapToResponse(app));
        }

        PageResponse<ApplicationResponse> response = new PageResponse<>();
        response.setItems(items);
        response.setTotal(appPage.getTotalElements());
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        response.setTotalPages(Math.max(1, appPage.getTotalPages()));
        return response;
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
        ApplicationResponse dto = new ApplicationResponse();
        dto.setId(a.getId());
        dto.setFio(a.getFio());
        dto.setPhone(a.getPhone());
        dto.setEmail(a.getEmail());
        dto.setRegistrationDate(a.getRegistrationDate());
        dto.setRegistrationTime(a.getRegistrationTime());
        dto.setStatus(a.getStatus());
        dto.setCreatedAt(a.getCreatedAt().atZone(ApplicationConfig.ZONE_MOSCOW).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return dto;
    }
}
