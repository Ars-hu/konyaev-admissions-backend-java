package ru.konyaev.priem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.stream.Collectors;

/**
 * Контроллер аутентификации администратора и выдачи JWT-токенов.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Авторизация администратора")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final ArchiveService archiveService;

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

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // Автоматическая архивация при входе
        archiveService.archiveExpired();

        log.info("Admin login successful: {}", loginRequest.getUsername());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .username(loginRequest.getUsername())
                .role(roles)
                .build());
    }
}
