package com.mottu.ops.domain.service;

import com.mottu.ops.domain.exception.DomainRuleException;
import com.mottu.ops.domain.exception.DomainValidationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class PasswordSecurityPolicy {

    public String computeSha256Hex(String rawPassword) {
        return computeHash(rawPassword, "SHA-256");
    }

    public String computeHash(String rawPassword, String algorithm) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou em branco.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new DomainValidationException("Algoritmo de hash inválido: " + algorithm);
        }
    }

    public void validatePasswordChange(String currentPassword, String newPassword, String existingPasswordSha256) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Senha atual é obrigatória.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new DomainValidationException("A nova senha deve ter pelo menos 8 caracteres.");
        }

        String newPasswordSha256 = computeSha256Hex(newPassword);
        String currentPasswordSha256 = computeSha256Hex(currentPassword);

        if (newPassword.equals(currentPassword) || newPasswordSha256.equalsIgnoreCase(currentPasswordSha256)) {
            throw new DomainRuleException("PASSWORD_REUSE_FORBIDDEN", "A nova senha deve ser diferente da senha atual.");
        }

        if (existingPasswordSha256 != null && !existingPasswordSha256.isBlank()) {
            if (newPasswordSha256.equalsIgnoreCase(existingPasswordSha256.trim())) {
                throw new DomainRuleException("PASSWORD_REUSE_FORBIDDEN", "A nova senha deve ser diferente da senha atual.");
            }
        }
    }
}
