package com.mottu.ops.application.port.out;

import com.mottu.ops.domain.enums.UserRole;

import java.util.UUID;

public interface TokenProviderPort {
    String generateToken(UUID userId, String email, UserRole role, String fullName);
    String generateExpiredToken(UUID userId, String email, UserRole role, String fullName);
    boolean validateToken(String token);
    UUID extractUserId(String token);
    String extractEmail(String token);
    UserRole extractRole(String token);
    String extractFullName(String token);
}
