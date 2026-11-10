package ru.konyaev.priem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.konyaev.priem.dto.RegisterRequest;
import ru.konyaev.priem.service.ApplicationService;
import ru.konyaev.priem.service.CalendarService;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для абитуриентов (публичный доступ без авторизации).
 */
@RestController
@Tag(name = "Public API", description = "Запись абитуриентов на приём и просмотр расписания")
public class PublicController {

    private final ApplicationService applicationService;
    private final CalendarService calendarService;

    @Autowired
    public PublicController(ApplicationService applicationService, CalendarService calendarService) {
        this.applicationService = applicationService;
        this.calendarService = calendarService;
    }

    @Operation(summary = "Записаться на приём")
    @PostMapping("/api/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        Map<String, Object> result = applicationService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Статус дат календаря")
    @GetMapping("/api/dates-status")
    public ResponseEntity<Map<String, Object>> getDatesStatus() {
        return ResponseEntity.ok(calendarService.getDatesStatus());
    }

    @Operation(summary = "Статус 10-минутных слотов на дату")
    @GetMapping("/api/slots-status/{date}")
    public ResponseEntity<Map<String, Object>> getSlotsStatus(@PathVariable String date) {
        return ResponseEntity.ok(calendarService.getSlotsStatus(date));
    }

    @Operation(summary = "Список открытых месяцев для записи")
    @GetMapping("/api/allowed-months")
    public ResponseEntity<List<Map<String, Object>>> getAllowedMonths() {
        return ResponseEntity.ok(calendarService.getAllowedMonths());
    }
}
