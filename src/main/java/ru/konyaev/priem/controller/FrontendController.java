package ru.konyaev.priem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Контроллер перенаправления на тестовый веб-интерфейс и гарантированной инициализации Swagger UI.
 */
@Controller
public class FrontendController {

    @GetMapping({"/", "/test", "/demo", "/java", "/java/", "/java/test"})
    public String index() {
        return "forward:/index.html";
    }

    /**
     * Гарантированная выдача корректного swagger-initializer.js для работы Swagger UI
     * через обратный прокси Nginx (/swagger-ui/ и /java/swagger-ui/).
     * Предотвращает ошибку "No API definition provided".
     */
    @GetMapping(value = {"/swagger-ui/swagger-initializer.js", "/java/swagger-ui/swagger-initializer.js"}, produces = "application/javascript;charset=UTF-8")
    @ResponseBody
    public String swaggerInitializer() {
        return """
            window.onload = function() {
              var isJava = window.location.pathname.indexOf('/java') === 0;
              var docUrl = isJava ? '/java/v3/api-docs' : '/v3/api-docs';
              var cfgUrl = isJava ? '/java/v3/api-docs/swagger-config' : '/v3/api-docs/swagger-config';

              window.ui = SwaggerUIBundle({
                url: docUrl,
                dom_id: '#swagger-ui',
                deepLinking: true,
                presets: [
                  SwaggerUIBundle.presets.apis,
                  SwaggerUIStandalonePreset
                ],
                plugins: [
                  SwaggerUIBundle.plugins.DownloadUrl
                ],
                layout: "StandaloneLayout",
                configUrl: cfgUrl,
                validatorUrl: "",
                displayRequestDuration: true,
                operationsSorter: "alpha",
                tagsSorter: "alpha"
              });
            };
            """;
    }
}
