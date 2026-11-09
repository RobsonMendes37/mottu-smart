package com.mottu.ops.domain.service;

import com.mottu.ops.domain.exception.DomainRuleException;
import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PasswordSecurityPolicyTest {

    private final PasswordSecurityPolicy policy = new PasswordSecurityPolicy();

    @Test
    @DisplayName("computeSha256Hex should return valid 64-character lowercase hexadecimal hash")
    void shouldComputeSha256HexProperly() {
        String hash = policy.computeSha256Hex("Mottu@2026!");
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("^[a-f0-9]{64}$");

        // Deterministic check for Mottu@2026!
        // echo -n 'Mottu@2026!' | sha256sum -> e4948ce65b16f387db2c451ca5d8009623e1f021e1a5fdfdfa15998df90ff5b0
        String secondHash = policy.computeSha256Hex("Mottu@2026!");
        assertThat(hash).isEqualTo(secondHash);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("computeSha256Hex should throw IllegalArgumentException for null, empty or blank strings")
    void shouldThrowWhenPasswordIsBlank(String blankPassword) {
        assertThatThrownBy(() -> policy.computeSha256Hex(blankPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Senha não pode ser nula ou em branco");
    }

    @Test
    @DisplayName("computeHash should throw DomainValidationException for invalid algorithm")
    void shouldThrowWhenAlgorithmIsInvalid() {
        assertThatThrownBy(() -> policy.computeHash("Mottu@2026!", "NON_EXISTENT_ALGO"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Algoritmo de hash inválido");
    }

    @Test
    @DisplayName("validatePasswordChange should succeed when all conditions are valid")
    void shouldValidatePasswordChangeSuccessfully() {
        String existingHash = policy.computeSha256Hex("Mottu@2026!");
        assertDoesNotThrow(() -> policy.validatePasswordChange("Mottu@2026!", "NovaSenha@2026#", existingHash));
    }

    @Test
    @DisplayName("validatePasswordChange should succeed when existingPasswordSha256 is null and new password is valid")
    void shouldValidatePasswordChangeWhenExistingHashIsNull() {
        assertDoesNotThrow(() -> policy.validatePasswordChange("Mottu@2026!", "NovaSenha@2026#", null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("validatePasswordChange should throw IllegalArgumentException when current password is blank")
    void shouldThrowWhenCurrentPasswordIsBlank(String blankCurrent) {
        assertThatThrownBy(() -> policy.validatePasswordChange(blankCurrent, "NovaSenha@2026#", "dummyHash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Senha atual é obrigatória");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1234567", "curta", "   "})
    @DisplayName("validatePasswordChange should throw DomainValidationException when new password has less than 8 characters")
    void shouldThrowWhenNewPasswordIsTooShort(String shortPassword) {
        assertThatThrownBy(() -> policy.validatePasswordChange("Mottu@2026!", shortPassword, "dummyHash"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("pelo menos 8 caracteres");
    }

    @Test
    @DisplayName("validatePasswordChange should throw DomainRuleException when new password equals current password")
    void shouldThrowWhenNewPasswordEqualsCurrentPassword() {
        String existingHash = policy.computeSha256Hex("Mottu@2026!");
        assertThatThrownBy(() -> policy.validatePasswordChange("Mottu@2026!", "Mottu@2026!", existingHash))
                .isInstanceOf(DomainRuleException.class)
                .hasFieldOrPropertyWithValue("code", "PASSWORD_REUSE_FORBIDDEN")
                .hasMessage("A nova senha deve ser diferente da senha atual.");
    }

    @Test
    @DisplayName("validatePasswordChange should throw DomainRuleException when new password SHA-256 matches existing hash")
    void shouldThrowWhenNewPasswordMatchesExistingSha256() {
        String existingHash = policy.computeSha256Hex("NovaSenha@2026#");
        assertThatThrownBy(() -> policy.validatePasswordChange("OutraSenha@123", "NovaSenha@2026#", existingHash))
                .isInstanceOf(DomainRuleException.class)
                .hasFieldOrPropertyWithValue("code", "PASSWORD_REUSE_FORBIDDEN")
                .hasMessage("A nova senha deve ser diferente da senha atual.");
    }
}

