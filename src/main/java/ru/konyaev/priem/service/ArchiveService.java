package ru.konyaev.priem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.konyaev.priem.config.ApplicationConfig;
import ru.konyaev.priem.dto.response.ArchivedApplicationResponse;
import ru.konyaev.priem.dto.response.PageResponse;
import ru.konyaev.priem.entity.Application;
import ru.konyaev.priem.entity.ArchivedApplication;
import ru.konyaev.priem.repository.ApplicationRepository;
import ru.konyaev.priem.repository.ArchivedApplicationRepository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис автоматического и ручного архивирования просроченных заявлений.
 * Фоновая задача запускается каждые 5 минут (@Scheduled).
 */
@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private final ApplicationRepository applicationRepository;
    private final ArchivedApplicationRepository archivedApplicationRepository;

    @Autowired
    public ArchiveService(
            ApplicationRepository applicationRepository,
            ArchivedApplicationRepository archivedApplicationRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.archivedApplicationRepository = archivedApplicationRepository;
    }

    /** Переносит заявки, время приёма которых уже наступило, в архивную таблицу. */
    @Transactional
    public int archiveExpired() {
        ZonedDateTime now = ZonedDateTime.now(ApplicationConfig.ZONE_MOSCOW);
        String todayStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        List<Application> expired = applicationRepository.findExpired(todayStr, currentTime);
        if (expired.isEmpty()) {
            return 0;
        }

        List<ArchivedApplication> toArchive = new ArrayList<>();
        for (Application app : expired) {
            ArchivedApplication archived = new ArchivedApplication();
            archived.setOriginalId(app.getId());
            archived.setFio(app.getFio());
            archived.setPhone(app.getPhone());
            archived.setEmail(app.getEmail());
            archived.setRegistrationDate(app.getRegistrationDate());
            archived.setRegistrationTime(app.getRegistrationTime());
            archived.setStatus(app.getStatus());
            archived.setCreatedAt(app.getCreatedAt());
            archived.setArchivedAt(Instant.now());
            toArchive.add(archived);
        }

        archivedApplicationRepository.saveAll(toArchive);
        applicationRepository.deleteAll(expired);

        log.info("Archive: moved {} expired application(s) to archive", expired.size());
        return expired.size();
    }

    /** Запускается фоном каждые 5 минут (300 000 миллисекунд). */
    @Scheduled(fixedDelay = 300000)
    public void scheduledArchive() {
        try {
            int count = archiveExpired();
            if (count > 0) {
                log.info("Scheduled archive completed: {} applications archived", count);
            }
        } catch (Exception e) {
            log.error("Scheduled archive error: {}", e.getMessage());
        }
    }

    public PageResponse<ArchivedApplicationResponse> getArchive(
            int page, int pageSize, String search, String sortField, String order
    ) {
        int safePage = Math.max(1, Math.min(page, 10000));
        int safePageSize = Math.max(1, Math.min(200, pageSize));

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort;
        if ("fio".equalsIgnoreCase(sortField)) {
            sort = Sort.by(direction, "fio");
        } else if ("registration_time".equalsIgnoreCase(sortField)) {
            sort = Sort.by(direction, "registrationTime");
        } else if ("archived_at".equalsIgnoreCase(sortField)) {
            sort = Sort.by(direction, "archivedAt");
        } else {
            sort = Sort.by(direction, "registrationDate");
        }

        Pageable pageable = PageRequest.of(safePage - 1, safePageSize, sort);
        Page<ArchivedApplication> resultPage;

        if (StringUtils.hasText(search)) {
            String safeSearch = search.trim();
            if (safeSearch.length() > 200) {
                safeSearch = safeSearch.substring(0, 200);
            }
            resultPage = archivedApplicationRepository.searchArchive(safeSearch, pageable);
        } else {
            resultPage = archivedApplicationRepository.findAll(pageable);
        }

        List<ArchivedApplicationResponse> items = new ArrayList<>();
        for (ArchivedApplication a : resultPage.getContent()) {
            items.add(mapToResponse(a));
        }

        PageResponse<ArchivedApplicationResponse> response = new PageResponse<>();
        response.setItems(items);
        response.setTotal(resultPage.getTotalElements());
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        response.setTotalPages(Math.max(1, resultPage.getTotalPages()));
        return response;
    }

    private ArchivedApplicationResponse mapToResponse(ArchivedApplication a) {
        ArchivedApplicationResponse dto = new ArchivedApplicationResponse();
        dto.setId(a.getId());
        dto.setOriginalId(a.getOriginalId());
        dto.setFio(a.getFio());
        dto.setPhone(a.getPhone());
        dto.setEmail(a.getEmail());
        dto.setRegistrationDate(a.getRegistrationDate());
        dto.setRegistrationTime(a.getRegistrationTime());
        dto.setStatus(a.getStatus());
        dto.setCreatedAt(a.getCreatedAt().atZone(ApplicationConfig.ZONE_MOSCOW).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        dto.setArchivedAt(a.getArchivedAt().atZone(ApplicationConfig.ZONE_MOSCOW).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return dto;
    }
}
