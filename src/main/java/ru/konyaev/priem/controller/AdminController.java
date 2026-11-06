package ru.konyaev.priem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.konyaev.priem.dto.request.*;
import ru.konyaev.priem.dto.response.*;
import ru.konyaev.priem.service.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin API", description = "Панель администратора приёмной комиссии: реестр заявок, слоты, архив, экспорт")
public class AdminController {

    private final ApplicationService applicationService;
    private final CalendarService calendarService;
    private final ArchiveService archiveService;
    private final ExportService exportService;
    private final SettingsService settingsService;

    // ─── Реестр заявлений ────────────────────────────────────────────────────

    @Operation(summary = "Получить список активных заявлений с пагинацией и поиском")
    @GetMapping("/applications")
    public ResponseEntity<PageResponse<ApplicationResponse>> getApplications(
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
            @Valid @RequestBody UpdateStatusRequest req
    ) {
        return ResponseEntity.ok(applicationService.updateStatus(id, req.getStatus()));
    }

    // ─── Архив ───────────────────────────────────────────────────────────────

    @Operation(summary = "Получить архивные заявления")
    @GetMapping("/archive")
    public ResponseEntity<PageResponse<ArchivedApplicationResponse>> getArchive(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "25") int pageSize,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "registration_date") String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        return ResponseEntity.ok(archiveService.getArchive(page, pageSize, search, sort, order));
    }

    @Operation(summary = "Ручной запуск архивации просроченных записей")
    @PostMapping("/archive/run")
    public ResponseEntity<Map<String, Integer>> runArchive() {
        int count = archiveService.archiveExpired();
        return ResponseEntity.ok(Map.of("archived", count));
    }

    // ─── Календарь и слоты ───────────────────────────────────────────────────

    @Operation(summary = "Получить заблокированные даты и открытые выходные")
    @GetMapping("/blocked-dates")
    public ResponseEntity<Map<String, List<String>>> getBlockedDates() {
        return ResponseEntity.ok(calendarService.getAdminBlockedDates());
    }

    @Operation(summary = "Переключить приём в выходной день")
    @PostMapping("/toggle-weekend")
    public ResponseEntity<Map<String, Object>> toggleWeekend(@Valid @RequestBody ToggleWeekendRequest req) {
        return ResponseEntity.ok(calendarService.toggleWeekend(req.getDate()));
    }

    @Operation(summary = "Заблокировать или разблокировать дату")
    @PostMapping("/toggle-date")
    public ResponseEntity<Map<String, Object>> toggleDate(@Valid @RequestBody ToggleDateRequest req) {
        return ResponseEntity.ok(calendarService.toggleDate(req.getDate()));
    }

    @Operation(summary = "Получить настройки слотов на дату")
    @GetMapping("/slot-configs/{date}")
    public ResponseEntity<Map<String, SlotInfoResponse>> getSlotConfigs(@PathVariable String date) {
        return ResponseEntity.ok(calendarService.getSlotConfigs(date));
    }

    @Operation(summary = "Обновить вместимость или заблокировать слот")
    @PostMapping("/update-slot")
    public ResponseEntity<Map<String, Object>> updateSlot(@Valid @RequestBody UpdateSlotRequest req) {
        return ResponseEntity.ok(calendarService.updateSlot(
                req.getDate(),
                req.getTime(),
                req.getMaxCapacity(),
                req.getIsBlocked()
        ));
    }

    @Operation(summary = "Получить открытые месяцы приёма")
    @GetMapping("/allowed-months")
    public ResponseEntity<List<AllowedMonthResponse>> getAllowedMonths() {
        return ResponseEntity.ok(calendarService.getAllowedMonths());
    }

    @Operation(summary = "Добавить месяц для приёма")
    @PostMapping("/add-month")
    public ResponseEntity<AllowedMonthResponse> addMonth(@Valid @RequestBody AddMonthRequest req) {
        AllowedMonthResponse response = calendarService.addMonth(req.getYear(), req.getMonth());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Удалить месяц из приёма")
    @PostMapping("/remove-month")
    public ResponseEntity<Map<String, Boolean>> removeMonth(@Valid @RequestBody RemoveMonthRequest req) {
        calendarService.removeMonth(req.getYear(), req.getMonth());
        return ResponseEntity.ok(Map.of("removed", true));
    }

    // ─── Экспорт в Excel (.xlsx) ─────────────────────────────────────────────

    @Operation(summary = "Выгрузка списка записей в формате Excel")
    @GetMapping("/applications/export/excel")
    public ResponseEntity<byte[]> exportApplicationsExcel(@RequestParam(required = false) String date) throws IOException {
        byte[] excelBytes = exportService.exportApplicationsExcel(date);

        String filename = (date != null && !date.isBlank())
                ? "записи_" + date + ".xlsx"
                : "все_записи.xlsx";

        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    // ─── Суточные лимиты ─────────────────────────────────────────────────────

    @Operation(summary = "Получить суточный лимит на дату")
    @GetMapping("/daily-limit/{date}")
    public ResponseEntity<DailyLimitResponse> getDailyLimit(@PathVariable String date) {
        return ResponseEntity.ok(calendarService.getDailyLimit(date));
    }

    @Operation(summary = "Установить суточный лимит на дату")
    @PostMapping("/daily-limit")
    public ResponseEntity<DailyLimitResponse> setDailyLimit(@Valid @RequestBody DailyLimitRequest req) {
        return ResponseEntity.ok(calendarService.setDailyLimit(req.getDate(), req.getMaxRegistrations()));
    }

    // ─── Настройки ───────────────────────────────────────────────────────────

    @Operation(summary = "Получить глобальные настройки")
    @GetMapping("/settings")
    public ResponseEntity<SiteSettingsResponse> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @Operation(summary = "Обновить глобальные настройки (advance_hours)")
    @PostMapping("/settings")
    public ResponseEntity<SiteSettingsResponse> updateSettings(@RequestBody UpdateSettingsRequest req) {
        if (req != null && req.getAdvanceHours() != null) {
            return ResponseEntity.ok(settingsService.updateAdvanceHours(req.getAdvanceHours()));
        }
        return ResponseEntity.ok(settingsService.getSettings());
    }
}
