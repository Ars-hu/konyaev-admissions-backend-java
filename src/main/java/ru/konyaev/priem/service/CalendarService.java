package ru.konyaev.priem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

/**
 * Сервис управления календарём приёма: проверка доступности дат и 10-минутных слотов,
 * блокировка дней, открытие рабочих суббот/воскресений, квоты и разрешённые месяцы.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final BlockedDateRepository blockedDateRepository;
    private final OpenedDateRepository openedDateRepository;
    private final AllowedMonthRepository allowedMonthRepository;
    private final SlotConfigRepository slotConfigRepository;
    private final DailyLimitRepository dailyLimitRepository;
    private final ApplicationRepository applicationRepository;
    private final SettingsService settingsService;

    /** Список годов и месяцев, открытых для подачи документов. */
    public List<AllowedMonthResponse> getAllowedMonths() {
        return allowedMonthRepository.findAllByOrderByYearAscMonthAsc().stream()
                .map(m -> AllowedMonthResponse.builder().year(m.getYear()).month(m.getMonth()).build())
                .toList();
    }

    /** Возвращает статус каждого из 37 слотов и суточного лимита на конкретную дату. */
    public SlotsStatusResponse getSlotsStatus(String date) {
        validateDateString(date);

        boolean dateBlocked = blockedDateRepository.existsByDate(date);

        List<Application> apps = applicationRepository.findByRegistrationDate(date);
        Map<String, Integer> occupiedMap = new HashMap<>();
        for (Application a : apps) {
            occupiedMap.put(a.getRegistrationTime(), occupiedMap.getOrDefault(a.getRegistrationTime(), 0) + 1);
        }

        List<SlotConfig> configs = slotConfigRepository.findByDate(date);
        Map<String, SlotConfig> configMap = configs.stream()
                .collect(Collectors.toMap(SlotConfig::getTime, c -> c));

        Map<String, SlotInfoResponse> slots = new LinkedHashMap<>();
        for (String time : ApplicationConfig.TIME_SLOTS) {
            SlotConfig cfg = configMap.get(time);
            int occupied = occupiedMap.getOrDefault(time, 0);
            int maxCap = (cfg != null && cfg.getMaxCapacity() != null) ? cfg.getMaxCapacity() : ApplicationConfig.DEFAULT_MAX_CAPACITY;
            boolean isBlocked = cfg != null && Boolean.TRUE.equals(cfg.getIsBlocked());

            slots.put(time, SlotInfoResponse.builder()
                    .occupied(occupied)
                    .maxCapacity(maxCap)
                    .isBlocked(isBlocked)
                    .build());
        }

        DailyLimit dailyLimit = dailyLimitRepository.findByDate(date).orElse(null);
        int maxReg = (dailyLimit != null && dailyLimit.getMaxRegistrations() != null) ? dailyLimit.getMaxRegistrations() : 0;
        long dailyCount = apps.size();

        return SlotsStatusResponse.builder()
                .dateBlocked(dateBlocked)
                .slots(slots)
                .dailyLimit(maxReg)
                .dailyCount(dailyCount)
                .build();
    }

    public DatesStatusResponse getDatesStatus() {
        List<String> blockedDates = blockedDateRepository.findAll().stream()
                .map(BlockedDate::getDate)
                .toList();

        List<String> openedWeekends = openedDateRepository.findAll().stream()
                .map(OpenedDate::getDate)
                .toList();

        List<ApplicationRepository.SlotCountView> counts = applicationRepository.countGroupedByDateAndTime();
        Map<String, Map<String, Long>> byDate = new HashMap<>();
        for (ApplicationRepository.SlotCountView sc : counts) {
            byDate.computeIfAbsent(sc.getDate(), k -> new HashMap<>()).put(sc.getTime(), sc.getCount());
        }

        List<SlotConfig> allConfigs = slotConfigRepository.findByDateIn(byDate.keySet());
        Map<String, SlotConfig> configMap = new HashMap<>();
        for (SlotConfig sc : allConfigs) {
            configMap.put(sc.getDate() + "_" + sc.getTime(), sc);
        }

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
                int maxCap = (cfg != null && cfg.getMaxCapacity() != null) ? cfg.getMaxCapacity() : ApplicationConfig.DEFAULT_MAX_CAPACITY;
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

        List<DailyLimit> activeLimits = dailyLimitRepository.findByMaxRegistrationsGreaterThan(0);
        for (DailyLimit dl : activeLimits) {
            long count = applicationRepository.countByRegistrationDate(dl.getDate());
            if (count >= dl.getMaxRegistrations()) {
                fullDates.add(dl.getDate());
            }
        }

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

        return DatesStatusResponse.builder()
                .blockedDates(blockedDates)
                .fullDates(new ArrayList<>(fullDates))
                .openedWeekends(openedWeekends)
                .blockedWithin24h(blockedWithinAdvance)
                .serverDate(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .serverNow(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
                .advanceHours(advanceHours)
                .build();
    }

    @Transactional
    public Map<String, Object> toggleDate(String date) {
        validateDateString(date);
        Optional<BlockedDate> existing = blockedDateRepository.findByDate(date);
        if (existing.isPresent()) {
            blockedDateRepository.delete(existing.get());
            return Map.of("blocked", false, "date", date);
        } else {
            blockedDateRepository.save(BlockedDate.builder().date(date).build());
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
            openedDateRepository.save(OpenedDate.builder().date(date).build());
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
        Map<String, SlotConfig> map = configs.stream().collect(Collectors.toMap(SlotConfig::getTime, c -> c));

        Map<String, SlotInfoResponse> result = new LinkedHashMap<>();
        for (String t : ApplicationConfig.TIME_SLOTS) {
            SlotConfig cfg = map.get(t);
            result.put(t, SlotInfoResponse.builder()
                    .maxCapacity(cfg != null && cfg.getMaxCapacity() != null ? cfg.getMaxCapacity() : ApplicationConfig.DEFAULT_MAX_CAPACITY)
                    .isBlocked(cfg != null && Boolean.TRUE.equals(cfg.getIsBlocked()))
                    .occupied(occupied.getOrDefault(t, 0))
                    .build());
        }
        return result;
    }

    @Transactional
    public Map<String, Object> updateSlot(String date, String time, Integer maxCapacity, Boolean isBlocked) {
        validateDateString(date);
        if (time == null || !ApplicationConfig.TIME_SLOTS.contains(time)) {
            throw new IllegalArgumentException("Недопустимое время");
        }

        SlotConfig cfg = slotConfigRepository.findByDateAndTime(date, time)
                .orElseGet(() -> SlotConfig.builder().date(date).time(time).build());

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
        AllowedMonth saved = allowedMonthRepository.save(AllowedMonth.builder().year(year).month(month).build());
        return AllowedMonthResponse.builder().year(saved.getYear()).month(saved.getMonth()).build();
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
        return DailyLimitResponse.builder()
                .date(date)
                .maxRegistrations(row != null ? row.getMaxRegistrations() : 0)
                .currentCount(count)
                .build();
    }

    @Transactional
    public DailyLimitResponse setDailyLimit(String date, int maxRegistrations) {
        validateDateString(date);
        if (maxRegistrations < 0 || maxRegistrations > 10000) {
            throw new IllegalArgumentException("Лимит должен быть от 0 до 10000");
        }
        DailyLimit row = dailyLimitRepository.findByDate(date)
                .orElseGet(() -> DailyLimit.builder().date(date).build());
        row.setMaxRegistrations(maxRegistrations);
        dailyLimitRepository.save(row);

        return DailyLimitResponse.builder()
                .date(date)
                .maxRegistrations(maxRegistrations)
                .build();
    }

    public Map<String, List<String>> getAdminBlockedDates() {
        return Map.of(
                "blocked_dates", blockedDateRepository.findAll().stream().map(BlockedDate::getDate).toList(),
                "opened_weekends", openedDateRepository.findAll().stream().map(OpenedDate::getDate).toList()
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
