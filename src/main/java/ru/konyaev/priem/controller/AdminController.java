package ru.konyaev.priem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.konyaev.priem.service.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Контроллер администратора (доступ только с JWT-токеном роли ROLE_ADMIN).
 */
@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin API", description = "Управление заявками, слотами, датами, архивом и экспорт в Excel")
public class AdminController {

    private final ApplicationService applicationService;
    private final CalendarService calendarService;
    private final ArchiveService archiveService;
    private final ExportService exportService;
    private final SettingsService settingsService;

    @Autowired
    public AdminController(
            ApplicationService applicationService,
            CalendarService calendarService,
            ArchiveService archiveService,
            ExportService exportService,
            SettingsService settingsService
    ) {
        this.applicationService = applicationService;
        this.calendarService = calendarService;
        this.archiveService = archiveService;
        this.exportService = exportService;
        this.settingsService = settingsService;
    }

    // ─── Заявления абитуриентов ──────────────────────────────────────────────

    @Operation(summary = "Список активных заявлений (поиск и пагинация)")
    @GetMapping("/applications")
    public ResponseEntity<Map<String, Object>> getApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "25") int pageSize,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "registration_date") String sort,
            @RequestParam(defaultValue = "asc") String order
    ) {
        return ResponseEntity.ok(applicationService.getApplications(page, pageSize, search, sort, order));
    }

    @Operation(summary = "Удалить заявление по ID")
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Map<String, Object>> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }

    @Operation(summary = "Изменить статус заявления (pending, confirmed, rejected)")
    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        String status = readRequiredString(body, "status", "Статус обязателен");
        return ResponseEntity.ok(applicationService.updateStatus(id, status));
    }

    @Operation(summary = "Скачать список заявлений в Excel (.xlsx)")
    @GetMapping("/applications/export/excel")
    public ResponseEntity<byte[]> exportApplicationsExcel(@RequestParam(required = false) String date) throws IOException {
        byte[] excelBytes = exportService.exportApplicationsExcel(date);
        String filename = (date != null && !date.isBlank()) ? "записи_" + date + ".xlsx" : "все_записи.xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    // ─── Архив ───────────────────────────────────────────────────────────────

    @Operation(summary = "Список архивных заявлений")
    @GetMapping("/archive")
    public ResponseEntity<Map<String, Object>> getArchive(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "25") int pageSize,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "registration_date") String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        return ResponseEntity.ok(archiveService.getArchive(page, pageSize, search, sort, order));
    }

    @Operation(summary = "Вручную перенести просроченные заявки в архив")
    @PostMapping("/archive/run")
    public ResponseEntity<Map<String, Integer>> runArchive() {
        int count = archiveService.archiveExpired();
        return ResponseEntity.ok(Map.of("archived_count", count));
    }

    // ─── Календарь и слоты ───────────────────────────────────────────────────

    @Operation(summary = "Список заблокированных дат и открытых выходных")
    @GetMapping("/blocked-dates")
    public ResponseEntity<Map<String, List<String>>> getBlockedDates() {
        return ResponseEntity.ok(calendarService.getAdminBlockedDates());
    }

    @Operation(summary = "Заблокировать или разблокировать дату")
    @PostMapping("/toggle-date")
    public ResponseEntity<Map<String, Object>> toggleDate(@RequestBody Map<String, Object> body) {
        String date = readRequiredString(body, "date", "Дата обязательна");
        return ResponseEntity.ok(calendarService.toggleDate(date));
    }

    @Operation(summary = "Открыть или закрыть приём в выходной день")
    @PostMapping("/toggle-weekend")
    public ResponseEntity<Map<String, Object>> toggleWeekend(@RequestBody Map<String, Object> body) {
        String date = readRequiredString(body, "date", "Дата обязательна");
        return ResponseEntity.ok(calendarService.toggleWeekend(date));
    }

    @Operation(summary = "Настройки слотов на выбранную дату")
    @GetMapping("/slot-configs/{date}")
    public ResponseEntity<Map<String, Map<String, Object>>> getSlotConfigs(@PathVariable String date) {
        return ResponseEntity.ok(calendarService.getSlotConfigs(date));
    }

    @Operation(summary = "Изменить вместимость или заблокировать слот")
    @PostMapping("/update-slot")
    public ResponseEntity<Map<String, Object>> updateSlot(@RequestBody Map<String, Object> body) {
        String date = readRequiredString(body, "date", "Дата обязательна");
        String time = readRequiredString(body, "time", "Время обязательно");
        Integer maxCapacity = readOptionalInt(body, "max_capacity");
        Boolean isBlocked = readOptionalBoolean(body, "is_blocked");
        return ResponseEntity.ok(calendarService.updateSlot(date, time, maxCapacity, isBlocked));
    }

    @Operation(summary = "Открытые месяцы приёма")
    @GetMapping("/allowed-months")
    public ResponseEntity<List<Map<String, Object>>> getAllowedMonths() {
        return ResponseEntity.ok(calendarService.getAllowedMonths());
    }

    @Operation(summary = "Добавить месяц приёма")
    @PostMapping("/add-month")
    public ResponseEntity<Map<String, Object>> addMonth(@RequestBody Map<String, Object> body) {
        int year = readRequiredInt(body, "year", "Год обязателен");
        int month = readRequiredInt(body, "month", "Месяц обязателен");
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarService.addMonth(year, month));
    }

    @Operation(summary = "Удалить месяц приёма")
    @PostMapping("/remove-month")
    public ResponseEntity<Map<String, Boolean>> removeMonth(@RequestBody Map<String, Object> body) {
        int year = readRequiredInt(body, "year", "Год обязателен");
        int month = readRequiredInt(body, "month", "Месяц обязателен");
        calendarService.removeMonth(year, month);
        return ResponseEntity.ok(Map.of("removed", true));
    }

    // ─── Дневные лимиты и настройки ──────────────────────────────────────────

    @Operation(summary = "Получить суточный лимит на дату")
    @GetMapping("/daily-limit/{date}")
    public ResponseEntity<Map<String, Object>> getDailyLimit(@PathVariable String date) {
        return ResponseEntity.ok(calendarService.getDailyLimit(date));
    }

    @Operation(summary = "Установить суточный лимит на дату")
    @PostMapping("/daily-limit")
    public ResponseEntity<Map<String, Object>> setDailyLimit(@RequestBody Map<String, Object> body) {
        String date = readRequiredString(body, "date", "Дата обязательна");
        int maxReg = readRequiredInt(body, "max_registrations", "Лимит обязателен");
        return ResponseEntity.ok(calendarService.setDailyLimit(date, maxReg));
    }

    @Operation(summary = "Получить настройки сайта")
    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @Operation(summary = "Обновить буфер времени записи (advance_hours)")
    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> body) {
        Integer hours = readOptionalInt(body, "advance_hours");
        if (hours != null) {
            return ResponseEntity.ok(settingsService.updateAdvanceHours(hours));
        }
        return ResponseEntity.ok(settingsService.getSettings());
    }

    // ─── Простые помощники для безопасного чтения JSON (чтобы не было 500 ошибок) ──

    private String readRequiredString(Map<String, Object> body, String key, String errorMsg) {
        if (body == null || body.get(key) == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        String val = body.get(key).toString().trim();
        if (val.isEmpty()) {
            throw new IllegalArgumentException(errorMsg);
        }
        return val;
    }

    private int readRequiredInt(Map<String, Object> body, String key, String errorMsg) {
        Integer val = readOptionalInt(body, key);
        if (val == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return val;
    }

    private Integer readOptionalInt(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object raw = body.get(key);
        if (raw instanceof Number num) {
            long val = num.longValue();
            if (val < Integer.MIN_VALUE || val > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Число '" + key + "' выходит за допустимые пределы");
            }
            return (int) val;
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Параметр '" + key + "' должен быть целым числом");
        }
    }

    private Boolean readOptionalBoolean(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object raw = body.get(key);
        if (raw instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(raw.toString().trim());
    }
}
