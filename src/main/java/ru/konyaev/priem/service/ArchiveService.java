package ru.konyaev.priem.service;

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
import ru.konyaev.priem.entity.Application;
import ru.konyaev.priem.entity.ArchivedApplication;
import ru.konyaev.priem.repository.ApplicationRepository;
import ru.konyaev.priem.repository.ArchivedApplicationRepository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис автоматического переноса старых заявок в архивную таблицу.
 */
@Service
public class ArchiveService {

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

    /** Переносит заявки, время которых уже прошло, в архив. */
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
        return expired.size();
    }

    /** Фоновая задача: запускается каждые 5 минут. */
    @Scheduled(fixedDelay = 300000)
    public void scheduledArchive() {
        try {
            archiveExpired();
        } catch (Exception ignored) {
        }
    }

    public Map<String, Object> getArchive(int page, int pageSize, String search, String sortField, String order) {
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

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", resultPage.getContent());
        response.put("total", resultPage.getTotalElements());
        response.put("page", safePage);
        response.put("page_size", safePageSize);
        response.put("total_pages", Math.max(1, resultPage.getTotalPages()));
        return response;
    }
}
