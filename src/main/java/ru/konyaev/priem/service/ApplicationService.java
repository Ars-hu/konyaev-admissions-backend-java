package ru.konyaev.priem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.konyaev.priem.config.ApplicationConfig;
import ru.konyaev.priem.dto.RegisterRequest;
import ru.konyaev.priem.entity.Application;
import ru.konyaev.priem.entity.DailyLimit;
import ru.konyaev.priem.entity.SlotConfig;
import ru.konyaev.priem.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Сервис работы с заявлениями абитуриентов:
 * запись с проверками, список для админки, смена статуса и удаление.
 */
@Service
public class ApplicationService {

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
     * Запись абитуриента на приём документов.
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Тело запроса отсутствует");
        }

        // 1. Проверяем обязательные поля и их длину
        String fio = req.getFio() != null ? req.getFio().trim() : "";
        String phone = req.getPhone() != null ? req.getPhone().trim() : "";
        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        String date = req.getRegistrationDate() != null ? req.getRegistrationDate().trim() : "";
        String time = req.getRegistrationTime() != null ? req.getRegistrationTime().trim() : "";

        if (fio.isBlank() || phone.isBlank() || email.isBlank() || date.isBlank() || time.isBlank()) {
            throw new IllegalArgumentException("Все поля обязательны для заполнения");
        }
        if (fio.length() > 255 || phone.length() > 20 || email.length() > 100) {
            throw new IllegalArgumentException("Превышена допустимая длина текстового поля");
        }
        if (!ApplicationConfig.TIME_SLOTS.contains(time)) {
            throw new IllegalArgumentException("Недопустимое время записи");
        }

        // 2. Проверяем формат даты и правило записи минимум за 24 часа
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

        LocalDateTime slotTime = LocalDateTime.parse(date + "T" + time + ":00");
        ZonedDateTime slotZdt = slotTime.atZone(ApplicationConfig.ZONE_MOSCOW);

        int advanceHours = settingsService.getAdvanceHours();
        if (!slotZdt.isAfter(now.plusHours(advanceHours))) {
            throw new IllegalArgumentException("Запись на это время закрыта (требуется оформление минимум за " + advanceHours + " ч до приёма).");
        }

        // 3. Проверяем календарь: открыт ли месяц, не выходной ли, не заблокирован ли день
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

        // 4. Проверяем блокировку слота и сколько в нём мест
        SlotConfig slotCfg = slotConfigRepository.findByDateAndTime(date, time).orElse(null);
        if (slotCfg != null && Boolean.TRUE.equals(slotCfg.getIsBlocked())) {
            throw new IllegalArgumentException("Запись на это время недоступна");
        }

        int maxCapacity = (slotCfg != null && slotCfg.getMaxCapacity() != null)
                ? slotCfg.getMaxCapacity()
                : ApplicationConfig.DEFAULT_MAX_CAPACITY;

        // 5. Защита от повторной записи тем же человеком на эту же дату
        if (applicationRepository.findExistingByDateAndPhoneOrEmail(date, phone, email).isPresent()) {
            throw new IllegalArgumentException("Вы уже записаны на эту дату");
        }

        // 6. Проверяем дневной лимит (если задан)
        DailyLimit dailyLimitRow = dailyLimitRepository.findByDate(date).orElse(null);
        if (dailyLimitRow != null && dailyLimitRow.getMaxRegistrations() != null && dailyLimitRow.getMaxRegistrations() > 0) {
            long dailyCount = applicationRepository.countByRegistrationDate(date);
            if (dailyCount >= dailyLimitRow.getMaxRegistrations()) {
                throw new IllegalArgumentException("На этот день достигнут лимит записей");
            }
        }

        // 7. Проверяем, остались ли свободные места в слоте
        long slotsCount = applicationRepository.countByRegistrationDateAndRegistrationTime(date, time);
        if (slotsCount >= maxCapacity) {
            throw new IllegalArgumentException("Извините, на это время мест больше нет");
        }

        // 8. Создаём объект и сохраняем в базу
        Application application = new Application();
        application.setFio(fio);
        application.setPhone(phone);
        application.setEmail(email);
        application.setRegistrationDate(date);
        application.setRegistrationTime(time);
        application.setStatus("pending");

        Application saved = applicationRepository.save(application);
        return Map.of("message", "Success", "id", saved.getId());
    }

    /** Список заявлений для админ-панели с поиском и страницами. */
    public Map<String, Object> getApplications(int page, int pageSize, String search, String sortField, String order) {
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

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", appPage.getContent());
        response.put("total", appPage.getTotalElements());
        response.put("page", safePage);
        response.put("page_size", safePageSize);
        response.put("total_pages", Math.max(1, appPage.getTotalPages()));
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
        if (newStatus == null) {
            throw new IllegalArgumentException("Статус обязателен");
        }
        String status = newStatus.trim().toLowerCase();
        if (!status.equals("pending") && !status.equals("confirmed") && !status.equals("rejected")) {
            throw new IllegalArgumentException("Недопустимый статус (ожидается pending, confirmed или rejected)");
        }

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Запись не найдена"));
        app.setStatus(status);
        applicationRepository.save(app);

        return Map.of("id", id, "status", app.getStatus());
    }

    public List<Application> getApplicationsForExport(String filterDate) {
        if (StringUtils.hasText(filterDate)) {
            return applicationRepository.findByRegistrationDateOrderByRegistrationDateAscRegistrationTimeAsc(filterDate.trim());
        }
        return applicationRepository.findAllByOrderByRegistrationDateAscRegistrationTimeAsc();
    }
}
