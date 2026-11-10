package ru.konyaev.priem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.konyaev.priem.dto.request.LoginRequest;
import ru.konyaev.priem.dto.response.AuthResponse;
import ru.konyaev.priem.security.JwtTokenProvider;
import ru.konyaev.priem.service.ArchiveService;

import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер аутентификации администратора и выдачи JWT-токенов.
 */
@RestController
@Tag(name = "Authentication", description = "Авторизация администратора")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final ArchiveService archiveService;

    @Autowired
    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            ArchiveService archiveService
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.archiveService = archiveService;
    }

    @Operation(summary = "Вход администратора", description = "Получение JWT токена для доступа к панели администратора")
    @PostMapping(value = {"/api/admin/login", "/api/v1/auth/login"})
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        List<String> roleList = new ArrayList<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            roleList.add(authority.getAuthority());
        }
        String roles = String.join(",", roleList);

        // Запускаем архивацию просроченных записей при входе в панель
        archiveService.archiveExpired();

        log.info("Admin login successful: {}", loginRequest.getUsername());

        return ResponseEntity.ok(new AuthResponse(token, loginRequest.getUsername(), roles));
    }
}
