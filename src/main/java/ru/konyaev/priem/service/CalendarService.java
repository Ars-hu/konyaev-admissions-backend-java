package ru.konyaev.priem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.konyaev.priem.config.ApplicationConfig;
import ru.konyaev.priem.dto.response.*;
import ru.konyaev.priem.entity.*;
import ru.konyaev.priem.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Сервис управления календарём приёма: проверка доступности дат и 10-минутных слотов,
 * блокировка дней, открытие рабочих выходных, суточные квоты и разрешённые месяцы.
 */
@Service
public class CalendarService {

    private final BlockedDateRepository blockedDateRepository;
    private final OpenedDateRepository openedDateRepository;
    private final AllowedMonthRepository allowedMonthRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final DailyLimitRepository dailyLimitRepository;
    private final ApplicationRepository applicationRepository;
    private final SettingsService settingsService;

    @Autowired
    public CalendarService(
            BlockedDateRepository blockedDateRepository,
            OpenedDateRepository openedDateRepository,
            AllowedMonthRepository allowedMonthRepository,
            SlotConfigRepository slotConfigRepository,
            DailyLimitRepository dailyLimitRepository,
            ApplicationRepository applicationRepository,
            SettingsService settingsService
    ) {
        this.blockedDateRepository = blockedDateRepository;
        this.openedDateRepository = openedDateRepository;
        this.allowedMonthRepository = allowedMonthRepository;
        this.slotConfigRepository = slotConfigRepository;
        this.dailyLimitRepository = dailyLimitRepository;
        this.applicationRepository = applicationRepository;
        this.settingsService = settingsService;
    }

    /** Список годов и месяцев, открытых для записи. */
    public List<AllowedMonthResponse> getAllowedMonths() {
        List<AllowedMonth> months = allowedMonthRepository.findAllByOrderByYearAscMonthAsc();
        List<AllowedMonthResponse> result = new ArrayList<>();
        for (AllowedMonth m : months) {
            result.add(new AllowedMonthResponse(m.getYear(), m.getMonth()));
        }
        return result;
    }

    /** Возвращает статус каждого из 37 слотов и суточного лимита на выбранную дату. */
    public SlotsStatusResponse getSlotsStatus(String date) {
        validateDateString(date);

        boolean dateBlocked = blockedDateRepository.existsByDate(date);

        // Считаем сколько человек уже записалось на каждое время в этот день
        List<Application> apps = applicationRepository.findByRegistrationDate(date);
        Map<String, Integer> occupiedMap = new HashMap<>();
        for (Application a : apps) {
            int current = occupiedMap.getOrDefault(a.getRegistrationTime(), 0);
            occupiedMap.put(a.getRegistrationTime(), current + 1);
        }

        // Загружаем индивидуальные настройки слотов (вместимость и блокировка)
        List<SlotConfig> configs = slotConfigRepository.findByDate(date);
        Map<String, SlotConfig> configMap = new HashMap<>();
        for (SlotConfig sc : configs) {
            configMap.put(sc.getTime(), sc);
        }

        // Формируем итоговую карту слотов с 09:00 до 15:00
        Map<String, SlotInfoResponse> slots = new LinkedHashMap<>();
        for (String time : ApplicationConfig.TIME_SLOTS) {
            SlotConfig cfg = configMap.get(time);
            int occupied = occupiedMap.getOrDefault(time, 0);
            int maxCap = (cfg != null && cfg.getMaxCapacity() != null)
                    ? cfg.getMaxCapacity()
                    : ApplicationConfig.DEFAULT_MAX_CAPACITY;
            boolean isBlocked = (cfg != null && Boolean.TRUE.equals(cfg.getIsBlocked()));

            SlotInfoResponse info = new SlotInfoResponse();
            info.setOccupied(occupied);
            info.setMaxCapacity(maxCap);
            info.setBlocked(isBlocked);
            slots.put(time, info);
        }

        DailyLimit dailyLimit = dailyLimitRepository.findByDate(date).orElse(null);
        int maxReg = (dailyLimit != null && dailyLimit.getMaxRegistrations() != null)
                ? dailyLimit.getMaxRegistrations()
                : 0;

        SlotsStatusResponse response = new SlotsStatusResponse();
        response.setDateBlocked(dateBlocked);
        response.setSlots(slots);
        response.setDailyLimit(maxReg);
        response.setDailyCount(apps.size());
        return response;
    }

    /** Собирает общую статистику календаря: заблокированные, заполненные и ближайшие недоступные даты. */
    public DatesStatusResponse getDatesStatus() {
        List<String> blockedDates = new ArrayList<>();
        for (BlockedDate b : blockedDateRepository.findAll()) {
            blockedDates.add(b.getDate());
        }

        List<String> openedWeekends = new ArrayList<>();
        for (OpenedDate o : openedDateRepository.findAll()) {
            openedWeekends.add(o.getDate());
        }

        // Группируем количество записей по (дата -> время -> количество)
        List<ApplicationRepository.SlotCountView> counts = applicationRepository.countGroupedByDateAndTime();
        Map<String, Map<String, Long>> byDate = new HashMap<>();
        for (ApplicationRepository.SlotCountView sc : counts) {
            Map<String, Long> timeMap = byDate.get(sc.getDate());
            if (timeMap == null) {
                timeMap = new HashMap<>();
                byDate.put(sc.getDate(), timeMap);
            }
            timeMap.put(sc.getTime(), sc.getCount());
        }

        List<SlotConfig> allConfigs = slotConfigRepository.findByDateIn(byDate.keySet());
        Map<String, SlotConfig> configMap = new HashMap<>();
        for (SlotConfig sc : allConfigs) {
            configMap.put(sc.getDate() + "_" + sc.getTime(), sc);
        }

        // Определяем даты, в которых заняты абсолютно все слоты
        Set<String> fullDates = new HashSet<>();
        for (Map.Entry<String, Map<String, Long>> entry : byDate.entrySet()) {
            String date = entry.getKey();
            Map<String, Long> timeCounts = entry.getValue();

            boolean allFull = true;
            for (String t : ApplicationConfig.TIME_SLOTS) {
                SlotConfig cfg = configMap.get(date + "_" + t);
                if (cfg != null && Boolean.TRUE.equals(cfg.getIsBlocked())) {
                    continue;
                }
                int maxCap = (cfg != null && cfg.getMaxCapacity() != null)
                        ? cfg.getMaxCapacity()
                        : ApplicationConfig.DEFAULT_MAX_CAPACITY;
                long curOccupied = timeCounts.getOrDefault(t, 0L);
                if (curOccupied < maxCap) {
                    allFull = false;
                    break;
                }
            }
            if (allFull) {
                fullDates.add(date);
            }
        }

        // Добавляем в заполненные те дни, где достигнут суточный лимит
        List<DailyLimit> activeLimits = dailyLimitRepository.findByMaxRegistrationsGreaterThan(0);
        for (DailyLimit dl : activeLimits) {
            long count = applicationRepository.countByRegistrationDate(dl.getDate());
            if (count >= dl.getMaxRegistrations()) {
                fullDates.add(dl.getDate());
            }
        }

        // Проверяем даты, которые полностью попадают в буфер 24 часов
        ZonedDateTime now = ZonedDateTime.now(ApplicationConfig.ZONE_MOSCOW);
        int advanceHours = settingsService.getAdvanceHours();
        ZonedDateTime cutoff = now.plusHours(advanceHours);

        List<String> blockedWithinAdvance = new ArrayList<>();
        String lastSlotTime = ApplicationConfig.TIME_SLOTS.get(ApplicationConfig.TIME_SLOTS.size() - 1);

        for (int delta = 0; delta < 2; delta++) {
            ZonedDateTime checkDay = now.plusDays(delta);
            String checkDateStr = checkDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            try {
                LocalDateTime lastSlotLdt = LocalDateTime.parse(checkDateStr + "T" + lastSlotTime + ":00");
                ZonedDateTime lastSlotZdt = lastSlotLdt.atZone(ApplicationConfig.ZONE_MOSCOW);

                if (!lastSlotZdt.isAfter(cutoff)) {
                    blockedWithinAdvance.add(checkDateStr);
                }
            } catch (Exception ignored) {
            }
        }

        DatesStatusResponse response = new DatesStatusResponse();
        response.setBlockedDates(blockedDates);
        response.setFullDates(new ArrayList<>(fullDates));
        response.setOpenedWeekends(openedWeekends);
        response.setBlockedWithin24h(blockedWithinAdvance);
        response.setServerDate(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        response.setServerNow(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        response.setAdvanceHours(advanceHours);
        return response;
    }

    @Transactional
    public Map<String, Object> toggleDate(String date) {
        validateDateString(date);
        Optional<BlockedDate> existing = blockedDateRepository.findByDate(date);
        if (existing.isPresent()) {
            blockedDateRepository.delete(existing.get());
            return Map.of("blocked", false, "date", date);
        } else {
            BlockedDate blockedDate = new BlockedDate();
            blockedDate.setDate(date);
            blockedDateRepository.save(blockedDate);
            return Map.of("blocked", true, "date", date);
        }
    }

    @Transactional
    public Map<String, Object> toggleWeekend(String date) {
        LocalDate parsed = validateDateString(date);
        if (parsed.getDayOfWeek() != DayOfWeek.SATURDAY && parsed.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Это не выходной день");
        }
        Optional<OpenedDate> existing = openedDateRepository.findByDate(date);
        if (existing.isPresent()) {
            openedDateRepository.delete(existing.get());
            return Map.of("opened", false, "date", date);
        } else {
            OpenedDate openedDate = new OpenedDate();
            openedDate.setDate(date);
            openedDateRepository.save(openedDate);
            return Map.of("opened", true, "date", date);
        }
    }

    public Map<String, SlotInfoResponse> getSlotConfigs(String date) {
        validateDateString(date);

        List<Application> apps = applicationRepository.findByRegistrationDate(date);
        Map<String, Integer> occupied = new HashMap<>();
        for (Application a : apps) {
            occupied.put(a.getRegistrationTime(), occupied.getOrDefault(a.getRegistrationTime(), 0) + 1);
        }

        List<SlotConfig> configs = slotConfigRepository.findByDate(date);
        Map<String, SlotConfig> configMap = new HashMap<>();
        for (SlotConfig sc : configs) {
            configMap.put(sc.getTime(), sc);
        }

        Map<String, SlotInfoResponse> result = new LinkedHashMap<>();
        for (String t : ApplicationConfig.TIME_SLOTS) {
            SlotConfig cfg = configMap.get(t);
            SlotInfoResponse info = new SlotInfoResponse();
            info.setMaxCapacity(cfg != null && cfg.getMaxCapacity() != null
                    ? cfg.getMaxCapacity()
                    : ApplicationConfig.DEFAULT_MAX_CAPACITY);
            info.setBlocked(cfg != null && Boolean.TRUE.equals(cfg.getIsBlocked()));
            info.setOccupied(occupied.getOrDefault(t, 0));
            result.put(t, info);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> updateSlot(String date, String time, Integer maxCapacity, Boolean isBlocked) {
        validateDateString(date);
        if (time == null || !ApplicationConfig.TIME_SLOTS.contains(time)) {
            throw new IllegalArgumentException("Недопустимое время");
        }

        SlotConfig cfg = slotConfigRepository.findByDateAndTime(date, time).orElse(null);
        if (cfg == null) {
            cfg = new SlotConfig();
            cfg.setDate(date);
            cfg.setTime(time);
        }

        if (maxCapacity != null) {
            if (maxCapacity < 1 || maxCapacity > 100) {
                throw new IllegalArgumentException("Вместимость слота должна быть от 1 до 100");
            }
            long curCount = applicationRepository.countByRegistrationDateAndRegistrationTime(date, time);
            if (maxCapacity < curCount) {
                throw new IllegalArgumentException(
                        "На это время уже записано " + curCount + " человек. Нельзя установить лимит меньше этого значения."
                );
            }
            cfg.setMaxCapacity(maxCapacity);
        }

        if (isBlocked != null) {
            cfg.setIsBlocked(isBlocked);
        }

        slotConfigRepository.save(cfg);

        return Map.of(
                "date", date,
                "time", time,
                "max_capacity", cfg.getMaxCapacity(),
                "is_blocked", cfg.getIsBlocked()
        );
    }

    @Transactional
    public AllowedMonthResponse addMonth(int year, int month) {
        if (month < 1 || month > 12 || year < 2020 || year > 2100) {
            throw new IllegalArgumentException("Некорректный месяц (1-12) или год (2020-2100)");
        }
        if (allowedMonthRepository.existsByYearAndMonth(year, month)) {
            throw new IllegalStateException("Этот месяц уже добавлен");
        }
        AllowedMonth newMonth = new AllowedMonth();
        newMonth.setYear(year);
        newMonth.setMonth(month);
        AllowedMonth saved = allowedMonthRepository.save(newMonth);
        return new AllowedMonthResponse(saved.getYear(), saved.getMonth());
    }

    @Transactional
    public void removeMonth(int year, int month) {
        if (month < 1 || month > 12 || year < 2020 || year > 2100) {
            throw new IllegalArgumentException("Некорректный месяц или год");
        }
        AllowedMonth row = allowedMonthRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new NoSuchElementException("Месяц не найден"));

        String prefix = String.format("%04d-%02d-", year, month);
        if (applicationRepository.existsByRegistrationDateStartingWith(prefix)) {
            throw new IllegalStateException("В этом месяце есть актуальные записи");
        }

        allowedMonthRepository.delete(row);
    }

    public DailyLimitResponse getDailyLimit(String date) {
        validateDateString(date);
        DailyLimit row = dailyLimitRepository.findByDate(date).orElse(null);
        long count = applicationRepository.countByRegistrationDate(date);

        DailyLimitResponse response = new DailyLimitResponse();
        response.setDate(date);
        response.setMaxRegistrations(row != null ? row.getMaxRegistrations() : 0);
        response.setCurrentCount(count);
        return response;
    }

    @Transactional
    public DailyLimitResponse setDailyLimit(String date, int maxRegistrations) {
        validateDateString(date);
        if (maxRegistrations < 0 || maxRegistrations > 10000) {
            throw new IllegalArgumentException("Лимит должен быть от 0 до 10000");
        }
        DailyLimit row = dailyLimitRepository.findByDate(date).orElse(null);
        if (row == null) {
            row = new DailyLimit();
            row.setDate(date);
        }
        row.setMaxRegistrations(maxRegistrations);
        dailyLimitRepository.save(row);

        DailyLimitResponse response = new DailyLimitResponse();
        response.setDate(date);
        response.setMaxRegistrations(maxRegistrations);
        return response;
    }

    public Map<String, List<String>> getAdminBlockedDates() {
        List<String> blocked = new ArrayList<>();
        for (BlockedDate b : blockedDateRepository.findAll()) {
            blocked.add(b.getDate());
        }

        List<String> opened = new ArrayList<>();
        for (OpenedDate o : openedDateRepository.findAll()) {
            opened.add(o.getDate());
        }

        return Map.of(
                "blocked_dates", blocked,
                "opened_weekends", opened
        );
    }

    private LocalDate validateDateString(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Дата обязательна");
        }
        if (date.length() > 20) {
            throw new IllegalArgumentException("Некорректная длина даты");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный формат даты (ожидается YYYY-MM-DD)");
        }
    }
}
