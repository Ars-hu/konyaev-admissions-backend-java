package ru.konyaev.priem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.konyaev.priem.config.ApplicationConfig;
import ru.konyaev.priem.entity.*;
import ru.konyaev.priem.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Сервис управления календарём: доступные месяцы, свободные слоты, блокировка дат и лимиты.
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

    /** Список открытых месяцев кампании. */
    public List<Map<String, Object>> getAllowedMonths() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AllowedMonth m : allowedMonthRepository.findAllByOrderByYearAscMonthAsc()) {
            result.add(Map.of("year", m.getYear(), "month", m.getMonth()));
        }
        return result;
    }

    /** Возвращает статус каждого из 37 слотов (занято / лимит / заблокировано) на указанную дату. */
    public Map<String, Object> getSlotsStatus(String date) {
        validateDateString(date);
        boolean dateBlocked = blockedDateRepository.existsByDate(date);

        List<Application> apps = applicationRepository.findByRegistrationDate(date);
        Map<String, Integer> occupiedMap = new HashMap<>();
        for (Application a : apps) {
            occupiedMap.put(a.getRegistrationTime(), occupiedMap.getOrDefault(a.getRegistrationTime(), 0) + 1);
        }

        List<SlotConfig> configs = slotConfigRepository.findByDate(date);
        Map<String, SlotConfig> configMap = new HashMap<>();
        for (SlotConfig sc : configs) {
            configMap.put(sc.getTime(), sc);
        }

        Map<String, Map<String, Object>> slots = new LinkedHashMap<>();
        for (String time : ApplicationConfig.TIME_SLOTS) {
            SlotConfig cfg = configMap.get(time);
            int occupied = occupiedMap.getOrDefault(time, 0);
            int maxCap = (cfg != null && cfg.getMaxCapacity() != null)
                    ? cfg.getMaxCapacity()
                    : ApplicationConfig.DEFAULT_MAX_CAPACITY;
            boolean isBlocked = (cfg != null && Boolean.TRUE.equals(cfg.getIsBlocked()));

            Map<String, Object> slotInfo = new LinkedHashMap<>();
            slotInfo.put("occupied", occupied);
            slotInfo.put("max_capacity", maxCap);
            slotInfo.put("is_blocked", isBlocked);
            slots.put(time, slotInfo);
        }

        DailyLimit dailyLimit = dailyLimitRepository.findByDate(date).orElse(null);
        int maxReg = (dailyLimit != null && dailyLimit.getMaxRegistrations() != null)
                ? dailyLimit.getMaxRegistrations()
                : 0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("date_blocked", dateBlocked);
        response.put("slots", slots);
        response.put("daily_limit", maxReg);
        response.put("daily_count", apps.size());
        return response;
    }

    /** Собирает сводный статус календаря: заблокированные дни, заполненные дни, выходные и серверное время. */
    public Map<String, Object> getDatesStatus() {
        List<String> blockedDates = new ArrayList<>();
        for (BlockedDate b : blockedDateRepository.findAll()) {
            blockedDates.add(b.getDate());
        }

        List<String> openedWeekends = new ArrayList<>();
        for (OpenedDate o : openedDateRepository.findAll()) {
            openedWeekends.add(o.getDate());
        }

        // Считаем сколько человек записалось на каждую пару (дата, время)
        List<Application> allApps = applicationRepository.findAll();
        Map<String, Map<String, Integer>> byDate = new HashMap<>();
        for (Application a : allApps) {
            Map<String, Integer> timeMap = byDate.get(a.getRegistrationDate());
            if (timeMap == null) {
                timeMap = new HashMap<>();
                byDate.put(a.getRegistrationDate(), timeMap);
            }
            timeMap.put(a.getRegistrationTime(), timeMap.getOrDefault(a.getRegistrationTime(), 0) + 1);
        }

        List<SlotConfig> allConfigs = slotConfigRepository.findAll();
        Map<String, SlotConfig> configMap = new HashMap<>();
        for (SlotConfig sc : allConfigs) {
            configMap.put(sc.getDate() + "_" + sc.getTime(), sc);
        }

        // Проверяем, где заняты абсолютно все слоты
        Set<String> fullDates = new HashSet<>();
        for (Map.Entry<String, Map<String, Integer>> entry : byDate.entrySet()) {
            String date = entry.getKey();
            Map<String, Integer> timeCounts = entry.getValue();

            boolean allFull = true;
            for (String t : ApplicationConfig.TIME_SLOTS) {
                SlotConfig cfg = configMap.get(date + "_" + t);
                if (cfg != null && Boolean.TRUE.equals(cfg.getIsBlocked())) {
                    continue;
                }
                int maxCap = (cfg != null && cfg.getMaxCapacity() != null)
                        ? cfg.getMaxCapacity()
                        : ApplicationConfig.DEFAULT_MAX_CAPACITY;
                if (timeCounts.getOrDefault(t, 0) < maxCap) {
                    allFull = false;
                    break;
                }
            }
            if (allFull) {
                fullDates.add(date);
            }
        }

        // Добавляем дни, где исчерпан общий суточный лимит
        for (DailyLimit dl : dailyLimitRepository.findByMaxRegistrationsGreaterThan(0)) {
            if (applicationRepository.countByRegistrationDate(dl.getDate()) >= dl.getMaxRegistrations()) {
                fullDates.add(dl.getDate());
            }
        }

        // Проверяем ближайшие 2 дня на буфер 24 часов
        ZonedDateTime now = ZonedDateTime.now(ApplicationConfig.ZONE_MOSCOW);
        int advanceHours = settingsService.getAdvanceHours();
        ZonedDateTime cutoff = now.plusHours(advanceHours);

        List<String> blockedWithinAdvance = new ArrayList<>();
        String lastSlotTime = ApplicationConfig.TIME_SLOTS.get(ApplicationConfig.TIME_SLOTS.size() - 1);

        for (int delta = 0; delta < 2; delta++) {
            String checkDate = now.plusDays(delta).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime lastSlotLdt = LocalDateTime.parse(checkDate + "T" + lastSlotTime + ":00");
            if (!lastSlotLdt.atZone(ApplicationConfig.ZONE_MOSCOW).isAfter(cutoff)) {
                blockedWithinAdvance.add(checkDate);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("blocked_dates", blockedDates);
        response.put("full_dates", new ArrayList<>(fullDates));
        response.put("opened_weekends", openedWeekends);
        response.put("blocked_within_24h", blockedWithinAdvance);
        response.put("server_date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        response.put("server_now", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        response.put("advance_hours", advanceHours);
        return response;
    }

    @Transactional
    public Map<String, Object> toggleDate(String date) {
        validateDateString(date);
        Optional<BlockedDate> existing = blockedDateRepository.findByDate(date);
        if (existing.isPresent()) {
            blockedDateRepository.delete(existing.get());
            return Map.of("blocked", false, "date", date, "message", "Дата разблокирована: " + date);
        }
        BlockedDate bd = new BlockedDate();
        bd.setDate(date);
        blockedDateRepository.save(bd);
        return Map.of("blocked", true, "date", date, "message", "Дата заблокирована: " + date);
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
            return Map.of("opened", false, "date", date, "message", "Выходной закрыт: " + date);
        }
        OpenedDate od = new OpenedDate();
        od.setDate(date);
        openedDateRepository.save(od);
        return Map.of("opened", true, "date", date, "message", "Выходной открыт для приёма: " + date);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getSlotConfigs(String date) {
        Map<String, Object> status = getSlotsStatus(date);
        return (Map<String, Map<String, Object>>) status.get("slots");
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
                throw new IllegalArgumentException("На это время уже записано " + curCount + " человек");
            }
            cfg.setMaxCapacity(maxCapacity);
        }

        if (isBlocked != null) {
            cfg.setIsBlocked(isBlocked);
        }

        slotConfigRepository.save(cfg);
        return Map.of("date", date, "time", time, "max_capacity", cfg.getMaxCapacity(), "is_blocked", cfg.getIsBlocked());
    }

    @Transactional
    public Map<String, Object> addMonth(int year, int month) {
        if (month < 1 || month > 12 || year < 2020 || year > 2100) {
            throw new IllegalArgumentException("Некорректный месяц (1-12) или год (2020-2100)");
        }
        if (allowedMonthRepository.existsByYearAndMonth(year, month)) {
            throw new IllegalStateException("Этот месяц уже добавлен");
        }
        AllowedMonth m = new AllowedMonth();
        m.setYear(year);
        m.setMonth(month);
        allowedMonthRepository.save(m);
        return Map.of("year", m.getYear(), "month", m.getMonth());
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

    public Map<String, Object> getDailyLimit(String date) {
        validateDateString(date);
        DailyLimit row = dailyLimitRepository.findByDate(date).orElse(null);
        long count = applicationRepository.countByRegistrationDate(date);
        return Map.of(
                "date", date,
                "max_registrations", row != null ? row.getMaxRegistrations() : 0,
                "current_count", count
        );
    }

    @Transactional
    public Map<String, Object> setDailyLimit(String date, int maxRegistrations) {
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

        return Map.of("date", date, "max_registrations", maxRegistrations);
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
        return Map.of("blocked_dates", blocked, "opened_weekends", opened);
    }

    private LocalDate validateDateString(String date) {
        if (date == null || date.isBlank() || date.length() > 20) {
            throw new IllegalArgumentException("Некорректная дата (ожидается YYYY-MM-DD)");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный формат даты (ожидается YYYY-MM-DD)");
        }
    }
}
