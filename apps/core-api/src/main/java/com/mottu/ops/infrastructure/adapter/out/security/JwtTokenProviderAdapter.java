package com.mottu.ops.infrastructure.adapter.out.security;

import com.mottu.ops.application.port.out.TokenProviderPort;
import com.mottu.ops.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProviderAdapter(
            @Value("${mottu.jwt.secret:trocar-em-producao-minimo-32-bytes-mottu-smart-ops-2026}") String secretString,
            @Value("${mottu.jwt.expiration-seconds:28800}") long expirationSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String generateToken(UUID userId, String email, UserRole role, String fullName) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("fullName", fullName)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    @Override
    public String generateExpiredToken(UUID userId, String email, UserRole role, String fullName) {
        Date now = new Date();
        Date past = new Date(now.getTime() - 3600000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("fullName", fullName)
                .issuedAt(new Date(past.getTime() - 3600000))
                .expiration(past)
                .signWith(key)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    @Override
    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    @Override
    public UserRole extractRole(String token) {
        Claims claims = parseClaims(token);
        String roleStr = claims.get("role", String.class);
        return UserRole.valueOf(roleStr);
    }

    @Override
    public String extractFullName(String token) {
        Claims claims = parseClaims(token);
        return claims.get("fullName", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
