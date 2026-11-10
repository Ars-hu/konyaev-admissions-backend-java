package ru.konyaev.priem;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс запуска Spring Boot приложения приёмной комиссии.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling // Включает фоновое авто-архивирование по расписанию (@Scheduled)
@OpenAPIDefinition(
        info = @Info(
                title = "Приёмная комиссия — ГБПОУ ТО «Тверской колледж им. А.Н. Коняева»",
                version = "1.0.0",
                description = "REST API электронной записи абитуриентов и панели администратора."
        ),
        servers = {
                @Server(url = "/java", description = "Через Nginx (/java)"),
                @Server(url = "/", description = "Прямой доступ")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class AdmissionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdmissionsApplication.class, args);
    }
}
