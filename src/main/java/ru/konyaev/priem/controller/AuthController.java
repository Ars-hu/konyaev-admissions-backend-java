package ru.konyaev.priem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.konyaev.priem.dto.LoginRequest;
import ru.konyaev.priem.entity.User;
import ru.konyaev.priem.repository.UserRepository;
import ru.konyaev.priem.security.JwtTokenProvider;
import ru.konyaev.priem.service.ArchiveService;

import java.util.Map;

/**
 * Вход администратора по логину и паролю с выдачей JWT-токена.
 */
@RestController
@Tag(name = "Authentication", description = "Вход администратора")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ArchiveService archiveService;

    @Autowired
    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            ArchiveService archiveService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.archiveService = archiveService;
    }

    @Operation(summary = "Войти в панель администратора")
    @PostMapping("/api/admin/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest loginRequest) {
        User admin = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);

        // Проверяем, что пользователь существует и пароль совпадает с хешем в базе
        if (admin == null || !passwordEncoder.matches(loginRequest.getPassword(), admin.getPasswordHash())) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        String token = tokenProvider.generateToken(admin.getUsername(), admin.getRole());

        // Заодно переносим просроченные записи в архив
        archiveService.archiveExpired();

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", admin.getUsername(),
                "role", admin.getRole()
        ));
    }
}
