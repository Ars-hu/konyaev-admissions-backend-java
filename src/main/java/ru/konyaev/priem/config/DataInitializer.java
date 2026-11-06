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

        // 2. Инициализация разрешённых месяцев (текущий, следующий и летняя кампания)
        try {
            LocalDate today = LocalDate.now(ApplicationConfig.ZONE_MOSCOW);
            int currentYear = today.getYear();
            int currentMonth = today.getMonthValue();

            // Текущий месяц и следующий месяц для круглогодичного тестирования и работы
            if (!allowedMonthRepository.existsByYearAndMonth(currentYear, currentMonth)) {
                allowedMonthRepository.save(AllowedMonth.builder().year(currentYear).month(currentMonth).build());
            }
            int nextMonth = currentMonth == 12 ? 1 : currentMonth + 1;
            int nextYear = currentMonth == 12 ? currentYear + 1 : currentYear;
            if (!allowedMonthRepository.existsByYearAndMonth(nextYear, nextMonth)) {
                allowedMonthRepository.save(AllowedMonth.builder().year(nextYear).month(nextMonth).build());
            }

            // Традиционные месяцы приёмной кампании (июнь, июль, август)
            int campaignYear = (currentMonth > 8) ? currentYear + 1 : currentYear;
            for (int m : new int[]{6, 7, 8}) {
                if (!allowedMonthRepository.existsByYearAndMonth(campaignYear, m)) {
                    allowedMonthRepository.save(AllowedMonth.builder().year(campaignYear).month(m).build());
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
