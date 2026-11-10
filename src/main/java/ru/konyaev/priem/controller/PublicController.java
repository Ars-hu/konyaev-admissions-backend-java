package ru.konyaev.priem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.konyaev.priem.dto.request.RegisterApplicationRequest;
import ru.konyaev.priem.dto.response.*;
import ru.konyaev.priem.service.ApplicationService;
import ru.konyaev.priem.service.CalendarService;

import java.util.List;
import java.util.Map;

/**
 * Презентационный слой (публичный): эндпоинты для абитуриентов без авторизации.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Public API", description = "Публичные методы для абитуриентов: запись, календарь, слоты")
public class PublicController {

    private final ApplicationService applicationService;
    private final CalendarService calendarService;

    @Operation(summary = "Регистрация заявления абитуриента", description = "Создание новой заявки на подачу документов")
    @PostMapping(value = {"/api/register", "/api/v1/applications/register"})
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterApplicationRequest req) {
        Map<String, Object> result = applicationService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Статус доступных дат для записи", description = "Список заблокированных, заполненных дат и открытых выходных")
    @GetMapping(value = {"/api/dates-status", "/api/v1/calendar/dates-status"})
    public ResponseEntity<DatesStatusResponse> getDatesStatus() {
        return ResponseEntity.ok(calendarService.getDatesStatus());
    }

    @Operation(summary = "Статус слотов на выбранную дату", description = "Количество мест и занятость 10-минутных интервалов")
    @GetMapping(value = {"/api/slots-status/{date}", "/api/v1/calendar/slots-status/{date}"})
    public ResponseEntity<SlotsStatusResponse> getSlotsStatus(@PathVariable String date) {
        return ResponseEntity.ok(calendarService.getSlotsStatus(date));
    }

    @Operation(summary = "Список открытых месяцев для приёма", description = "Месяцы, доступные для подачи заявлений")
    @GetMapping(value = {"/api/allowed-months", "/api/v1/calendar/allowed-months"})
    public ResponseEntity<List<AllowedMonthResponse>> getAllowedMonths() {
        return ResponseEntity.ok(calendarService.getAllowedMonths());
    }
}
