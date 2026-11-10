package ru.konyaev.priem.config;

import java.time.ZoneId;
import java.util.List;

/**
 * Базовые константы и справочники расписания приёмной комиссии.
 */
public class ApplicationConfig {

    /** Часовой пояс колледжа (Тверь / Москва). */
    public static final ZoneId ZONE_MOSCOW = ZoneId.of("Europe/Moscow");

    /** Стандартная вместимость одного 10-минутного слота (человек). */
    public static final int DEFAULT_MAX_CAPACITY = 3;

    /** Расписание приёма: 37 интервалов по 10 минут с 09:00 до 15:00. */
    public static final List<String> TIME_SLOTS = List.of(
            "09:00", "09:10", "09:20", "09:30", "09:40", "09:50",
            "10:00", "10:10", "10:20", "10:30", "10:40", "10:50",
            "11:00", "11:10", "11:20", "11:30", "11:40", "11:50",
            "12:00", "12:10", "12:20", "12:30", "12:40", "12:50",
            "13:00", "13:10", "13:20", "13:30", "13:40", "13:50",
            "14:00", "14:10", "14:20", "14:30", "14:40", "14:50",
            "15:00"
    );

    public static final List<String> RU_MONTHS = List.of(
            "",
            "января",
            "февраля",
            "марта",
            "апреля",
            "мая",
            "июня",
            "июля",
            "августа",
            "сентября",
            "октября",
            "ноября",
            "декабря"
    );

    public static String formatRussianDate(String dateStr) {
        if (dateStr == null || !dateStr.contains("-")) {
            return dateStr;
        }
        try {
            String[] parts = dateStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            if (month >= 1 && month <= 12) {
                return day + " " + RU_MONTHS.get(month) + " " + year;
            }
        } catch (Exception ignored) {
        }
        return dateStr;
    }
}
