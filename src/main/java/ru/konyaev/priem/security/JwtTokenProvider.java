package ru.konyaev.priem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Утилита для генерации, подписи (HMAC-SHA256) и проверки JWT-токенов администратора.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:default_dev_jwt_secret_key_change_in_prod_32bytes}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        this.expirationMs = expirationMs;
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // Для HMAC-SHA256 требуется ключ длиной минимум 32 байта (256 бит)
        if (keyBytes.length < 32) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Создаёт JWT-токен на основе успешной аутентификации Spring Security. */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        List<String> roleList = new ArrayList<>();
        for (GrantedAuthority authority : userPrincipal.getAuthorities()) {
            roleList.add(authority.getAuthority());
        }
        String roles = String.join(",", roleList);
        return generateToken(userPrincipal.getUsername(), roles);
    }

    /** Генерирует подписанный JWT-токен со сроком действия (по умолчанию 24 часа). */
    public String generateToken(String username, String roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("user", username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /** Извлекает логин (username) из валидного JWT-токена. */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userClaim = claims.get("user", String.class);
        if (userClaim != null && !userClaim.isBlank()) {
            return userClaim;
        }
        return claims.getSubject();
    }

    /** Проверяет подпись и срок действия токена. */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
