package ru.konyaev.priem.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.konyaev.priem.config.ApplicationConfig;
import ru.konyaev.priem.entity.Application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Сервис выгрузки списка заявлений в Excel-файл (.xlsx) через Apache POI.
 */
@Service
public class ExportService {

    private final ApplicationService applicationService;

    private static final Map<String, String> STATUS_RU = Map.of(
            "pending", "Ожидает",
            "confirmed", "Подтверждена",
            "rejected", "Отклонена"
    );

    @Autowired
    public ExportService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public byte[] exportApplicationsExcel(String filterDate) throws IOException {
        List<Application> apps = applicationService.getApplicationsForExport(filterDate);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Записи");

            // Первая строка — заголовки столбцов
            Row header = sheet.createRow(0);
            String[] titles = {"ID", "ФИО", "Телефон", "Email", "Дата записи", "Время", "Статус", "Дата создания"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }

            // Заполняем таблицу заявками по строкам
            int rowNum = 1;
            for (Application a : apps) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getId() != null ? a.getId() : 0);
                row.createCell(1).setCellValue(a.getFio() != null ? a.getFio() : "");
                row.createCell(2).setCellValue(a.getPhone() != null ? a.getPhone() : "");
                row.createCell(3).setCellValue(a.getEmail() != null ? a.getEmail() : "");
                row.createCell(4).setCellValue(a.getRegistrationDate() != null ? a.getRegistrationDate() : "");
                row.createCell(5).setCellValue(a.getRegistrationTime() != null ? a.getRegistrationTime() : "");

                String st = a.getStatus() != null ? a.getStatus() : "";
                row.createCell(6).setCellValue(STATUS_RU.getOrDefault(st, st));

                String created = (a.getCreatedAt() != null)
                        ? a.getCreatedAt().atZone(ApplicationConfig.ZONE_MOSCOW).format(fmt)
                        : "";
                row.createCell(7).setCellValue(created);
            }

            for (int i = 0; i < titles.length; i++) {
                sheet.setColumnWidth(i, 22 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
