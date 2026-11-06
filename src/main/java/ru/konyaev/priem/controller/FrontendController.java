package ru.konyaev.priem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер перенаправления на тестовый веб-интерфейс.
 */
@Controller
public class FrontendController {

    @GetMapping({"/", "/test", "/demo", "/java", "/java/", "/java/test"})
    public String index() {
        return "forward:/index.html";
    }
}
