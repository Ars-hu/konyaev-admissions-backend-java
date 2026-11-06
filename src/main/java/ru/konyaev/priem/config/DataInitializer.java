package ru.konyaev.priem.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.konyaev.priem.entity.AllowedMonth;
import ru.konyaev.priem.entity.Role;
import ru.konyaev.priem.entity.User;
import ru.konyaev.priem.repository.AllowedMonthRepository;
import ru.konyaev.priem.repository.UserRepository;
import ru.konyaev.priem.service.ArchiveService;
import ru.konyaev.priem.service.SettingsService;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AllowedMonthRepository allowedMonthRepository;
    private final ArchiveService archiveService;
    private final SettingsService settingsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        log.info("Starting Konyaev College Admissions Backend initialization...");

        // 1. Автоматическая архивация просроченных записей
        try {
            archiveService.archiveExpired();
        } catch (Exception e) {
            log.warn("Startup archive check failed: {}", e.getMessage());
        }

        // 2. Инициализация разрешённых месяцев (6, 7, 8) если таблица пуста
        try {
            if (allowedMonthRepository.count() == 0) {
                int currentYear = LocalDate.now(ApplicationConfig.ZONE_MOSCOW).getYear();
                for (int m : new int[]{6, 7, 8}) {
                    allowedMonthRepository.save(AllowedMonth.builder().year(currentYear).month(m).build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to initialize allowed months: {}", e.getMessage());
        }

        // 3. Создание или обновление учетной записи администратора
        try {
            User admin = userRepository.findByUsername(adminUsername).orElse(null);
            if (admin == null) {
                admin = User.builder()
                        .username(adminUsername)
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .role(Role.ROLE_ADMIN)
                        .build();
                userRepository.save(admin);
                log.info("Initialized default administrator account: {}", adminUsername);
            } else {
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ROLE_ADMIN);
                userRepository.save(admin);
            }
        } catch (Exception e) {
            log.error("Failed to initialize admin account: {}", e.getMessage());
        }

        // 4. Настройки по умолчанию
        try {
            if (settingsService.get("advance_hours", null) == null) {
                settingsService.set("advance_hours", "24");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize default settings: {}", e.getMessage());
        }

        log.info("Konyaev College Admissions Backend initialized successfully!");
    }
}
