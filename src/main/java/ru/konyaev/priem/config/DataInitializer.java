package ru.konyaev.priem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.konyaev.priem.entity.AllowedMonth;
import ru.konyaev.priem.entity.User;
import ru.konyaev.priem.repository.AllowedMonthRepository;
import ru.konyaev.priem.repository.UserRepository;
import ru.konyaev.priem.service.ArchiveService;
import ru.konyaev.priem.service.SettingsService;

import java.time.LocalDate;

/**
 * Выполняется один раз при запуске сервера: создаёт администратора, настройки по умолчанию и месяцы приёма.
 */
@Component
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

    @Autowired
    public DataInitializer(
            UserRepository userRepository,
            AllowedMonthRepository allowedMonthRepository,
            ArchiveService archiveService,
            SettingsService settingsService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.allowedMonthRepository = allowedMonthRepository;
        this.archiveService = archiveService;
        this.settingsService = settingsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Архивируем старые заявки при старте
        archiveService.archiveExpired();

        // 2. Открываем текущий, следующий месяц и летнюю кампанию (июнь, июль, август)
        LocalDate today = LocalDate.now(ApplicationConfig.ZONE_MOSCOW);
        addMonthIfMissing(today.getYear(), today.getMonthValue());
        LocalDate nextMonth = today.plusMonths(1);
        addMonthIfMissing(nextMonth.getYear(), nextMonth.getMonthValue());

        int campaignYear = (today.getMonthValue() > 8) ? today.getYear() + 1 : today.getYear();
        addMonthIfMissing(campaignYear, 6);
        addMonthIfMissing(campaignYear, 7);
        addMonthIfMissing(campaignYear, 8);

        // 3. Создаём или обновляем администратора с зашифрованным паролем BCrypt
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setUsername(adminUsername);
        }
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole("ROLE_ADMIN");
        userRepository.save(admin);

        // 4. Настройка буфера записи за 24 часа
        if (settingsService.get("advance_hours", null) == null) {
            settingsService.set("advance_hours", "24");
        }
    }

    private void addMonthIfMissing(int year, int month) {
        if (!allowedMonthRepository.existsByYearAndMonth(year, month)) {
            AllowedMonth m = new AllowedMonth();
            m.setYear(year);
            m.setMonth(month);
            allowedMonthRepository.save(m);
        }
    }
}
