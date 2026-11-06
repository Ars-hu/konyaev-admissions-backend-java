package ru.konyaev.priem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Приёмная комиссия — ГБПОУ ТО «Тверской колледж им. А.Н. Коняева»")
                        .description("REST API бэкенда приёмной комиссии колледжа на Java Spring Boot 3. " +
                                "3-х слойная архитектура, ролевая модель (Абитуриент и Администратор), " +
                                "JWT авторизация, бронирование слотов, лимиты, регламентное авто-архивирование и экспорт в Excel.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Приёмная комиссия Колледжа им. Коняева")
                                .url("https://priem-konyaeva.ru")
                                .email("priem@konyaev.ru"))
                        .license(new License().name("Apache 2.0").url("https://spring.io")))
                .servers(List.of(
                        new Server().url("/java").description("Проксированный контур Nginx (/java)"),
                        new Server().url("/").description("Прямой доступ (Root)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
