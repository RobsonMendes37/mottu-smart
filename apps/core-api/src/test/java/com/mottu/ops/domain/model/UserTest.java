package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.UserRole;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    @DisplayName("Should create user with valid fields")
    void shouldCreateUser() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        User user = new User(
                id,
                "Carlos Silva",
                "12345678901",
                "+5511999998888",
                "carlos@mottu.com",
                "$2a$10$hashedpassword",
                UserRole.RIDER,
                AccountStatus.PENDING_APPROVAL,
                null,
                null,
                now
        );

        assertEquals(id, user.getId());
        assertEquals("Carlos Silva", user.getFullName());
        assertEquals("12345678901", user.getCpf());
        assertEquals("+5511999998888", user.getPhone());
        assertEquals("carlos@mottu.com", user.getEmail());
        assertEquals("$2a$10$hashedpassword", user.getPasswordHash());
        assertEquals(UserRole.RIDER, user.getRole());
        assertEquals(AccountStatus.PENDING_APPROVAL, user.getStatus());
        assertNull(user.getRejectionReason());
        assertNull(user.getApprovedBy());
        assertEquals(now, user.getCreatedAt());
        assertFalse(user.isActive());
    }

    @Test
    @DisplayName("approve changes status to ACTIVE and records manager ID")
    void shouldApproveUser() {
        User user = new User(
                null,
                "Carlos Silva",
                "12345678901",
                "+5511999998888",
                "carlos@mottu.com",
                "hashed",
                UserRole.RIDER,
                AccountStatus.PENDING_APPROVAL,
                null,
                null,
                null
        );

        UUID managerId = UUID.randomUUID();
        user.approve(managerId);

        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertEquals(managerId, user.getApprovedBy());
        assertNull(user.getRejectionReason());
        assertTrue(user.isActive());

        // Approving again throws BusinessRuleViolationException
        assertThrows(BusinessRuleViolationException.class, () -> user.approve(managerId));
    }

    @Test
    @DisplayName("reject changes status to REJECTED and records manager ID and reason")
    void shouldRejectUser() {
        User user = new User(
                null,
                "Carlos Silva",
                "12345678901",
                "+5511999998888",
                "carlos@mottu.com",
                "hashed",
                UserRole.RIDER,
                AccountStatus.PENDING_APPROVAL,
                null,
                null,
                null
        );

        UUID managerId = UUID.randomUUID();
        user.reject(managerId, "CNH vencida");

        assertEquals(AccountStatus.REJECTED, user.getStatus());
        assertEquals(managerId, user.getApprovedBy());
        assertEquals("CNH vencida", user.getRejectionReason());
        assertFalse(user.isActive());

        // Rejecting again throws BusinessRuleViolationException
        assertThrows(BusinessRuleViolationException.class, () -> user.reject(managerId, "outra razão"));
    }

    @Test
    @DisplayName("Constructor parameter validations")
    void shouldValidateConstructorParameters() {
        UUID validId = UUID.randomUUID();

        // null or blank fullName
        assertThrows(DomainValidationException.class, () ->
                new User(validId, null, "12345678901", "11999998888", "a@b.com", "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "  ", "12345678901", "11999998888", "a@b.com", "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));

        // invalid cpf (not 11 digits)
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", null, "11999998888", "a@b.com", "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345", "11999998888", "a@b.com", "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "123456789012", "11999998888", "a@b.com", "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));

        // null or blank phone
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345678901", null, "a@b.com", "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345678901", "   ", "a@b.com", "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));

        // invalid email
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345678901", "11999998888", null, "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345678901", "11999998888", "invalidemail", "h", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));

        // null or blank passwordHash
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345678901", "11999998888", "a@b.com", null, UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345678901", "11999998888", "a@b.com", "  ", UserRole.RIDER, AccountStatus.ACTIVE, null, null, null));

        // null role or status
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345678901", "11999998888", "a@b.com", "h", null, AccountStatus.ACTIVE, null, null, null));
        assertThrows(DomainValidationException.class, () ->
                new User(validId, "Name", "12345678901", "11999998888", "a@b.com", "h", UserRole.RIDER, null, null, null, null));
    }
}
