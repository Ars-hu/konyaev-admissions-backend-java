package ru.konyaev.priem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ApplicationRepository applicationRepository;
    private final ArchivedApplicationRepository archivedApplicationRepository;

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
            toArchive.add(ArchivedApplication.builder()
                    .originalId(app.getId())
                    .fio(app.getFio())
                    .phone(app.getPhone())
                    .email(app.getEmail())
                    .registrationDate(app.getRegistrationDate())
                    .registrationTime(app.getRegistrationTime())
                    .status(app.getStatus())
                    .createdAt(app.getCreatedAt())
                    .archivedAt(Instant.now())
                    .build());
        }

        archivedApplicationRepository.saveAll(toArchive);
        applicationRepository.deleteAll(expired);

        log.info("Archive: moved {} expired application(s) to archive", expired.size());
        return expired.size();
    }

    @Scheduled(fixedDelay = 300000) // Каждые 5 минут
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

        Specification<ArchivedApplication> spec = Specification.where(null);
        if (StringUtils.hasText(search)) {
            String safeSearch = search.trim();
            if (safeSearch.length() > 200) safeSearch = safeSearch.substring(0, 200);
            String like = "%" + safeSearch.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fio")), like),
                    cb.like(cb.lower(root.get("phone")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("registrationDate")), like)
            ));
        }

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
        Page<ArchivedApplication> resultPage = archivedApplicationRepository.findAll(spec, pageable);

        List<ArchivedApplicationResponse> items = resultPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<ArchivedApplicationResponse>builder()
                .items(items)
                .total(resultPage.getTotalElements())
                .page(safePage)
                .pageSize(safePageSize)
                .totalPages(Math.max(1, resultPage.getTotalPages()))
                .build();
    }

    private ArchivedApplicationResponse mapToResponse(ArchivedApplication a) {
        return ArchivedApplicationResponse.builder()
                .id(a.getId())
                .originalId(a.getOriginalId())
                .fio(a.getFio())
                .phone(a.getPhone())
                .email(a.getEmail())
                .registrationDate(a.getRegistrationDate())
                .registrationTime(a.getRegistrationTime())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt().atZone(ApplicationConfig.ZONE_MOSCOW).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .archivedAt(a.getArchivedAt().atZone(ApplicationConfig.ZONE_MOSCOW).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }
}
