package ru.konyaev.priem.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.konyaev.priem.config.ApplicationConfig;
import ru.konyaev.priem.entity.Application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Сервис формирования официальных ведомостей приёма в формате Excel (.xlsx)
 * с помощью библиотеки Apache POI.
 */
@Service
public class ExportService {

    private final ApplicationService applicationService;

    private static final Map<String, String> STATUS_RU = Map.of(
            "pending", "Ожидает",
            "confirmed", "Подтверждена",
            "rejected", "Отклонена"
    );

    private static final DateTimeFormatter FORMATTER_DATETIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    public ExportService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** Генерирует байты .xlsx файла со списком заявок (всех или на выбранную дату). */
    public byte[] exportApplicationsExcel(String filterDate) throws IOException {
        List<Application> apps = applicationService.getApplicationsForExport(filterDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Записи");

            // Стиль для синей шапки таблицы
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            String[] headers = {"ID", "ФИО", "Телефон", "Email", "Дата записи", "Время", "Статус", "Дата создания"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Application a : apps) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                Cell c0 = row.createCell(0);
                c0.setCellValue(a.getId() != null ? a.getId() : 0);
                c0.setCellStyle(dataStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(a.getFio() != null ? a.getFio() : "");
                c1.setCellStyle(dataStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(a.getPhone() != null ? a.getPhone() : "");
                c2.setCellStyle(dataStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(a.getEmail() != null ? a.getEmail() : "");
                c3.setCellStyle(dataStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(a.getRegistrationDate() != null ? a.getRegistrationDate() : "");
                c4.setCellStyle(dataStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(a.getRegistrationTime() != null ? a.getRegistrationTime() : "");
                c5.setCellStyle(dataStyle);

                Cell c6 = row.createCell(6);
                String statusStr = a.getStatus() != null ? a.getStatus() : "";
                c6.setCellValue(STATUS_RU.getOrDefault(statusStr, statusStr));
                c6.setCellStyle(dataStyle);

                Cell c7 = row.createCell(7);
                String createdStr = "";
                if (a.getCreatedAt() != null) {
                    ZonedDateTime createdZdt = a.getCreatedAt().atZone(ApplicationConfig.ZONE_MOSCOW);
                    createdStr = createdZdt.format(FORMATTER_DATETIME);
                }
                c7.setCellValue(createdStr);
                c7.setCellStyle(dataStyle);
            }

            // Ширина столбцов таблицы
            int[] colWidths = {6, 35, 18, 30, 14, 10, 16, 18};
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i] * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
