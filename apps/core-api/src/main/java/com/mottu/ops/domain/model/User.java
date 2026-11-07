package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.UserRole;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final UUID id;
    private final String fullName;
    private final String cpf;
    private final String phone;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private AccountStatus status;
    private String rejectionReason;
    private UUID approvedBy;
    private final Instant createdAt;

    public User(
            UUID id,
            String fullName,
            String cpf,
            String phone,
            String email,
            String passwordHash,
            UserRole role,
            AccountStatus status,
            String rejectionReason,
            UUID approvedBy,
            Instant createdAt
    ) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainValidationException("Full name cannot be blank");
        }
        if (cpf == null || !cpf.matches("\\d{11}")) {
            throw new DomainValidationException("CPF must contain exactly 11 digits");
        }
        if (phone == null || phone.isBlank()) {
            throw new DomainValidationException("Phone cannot be blank");
        }
        if (email == null || !email.contains("@")) {
            throw new DomainValidationException("Invalid email format");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainValidationException("Password hash cannot be blank");
        }
        if (role == null) {
            throw new DomainValidationException("User role cannot be null");
        }
        if (status == null) {
            throw new DomainValidationException("Account status cannot be null");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.fullName = fullName.trim();
        this.cpf = cpf;
        this.phone = phone.trim();
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.approvedBy = approvedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void approve(UUID managerId) {
        if (this.status != AccountStatus.PENDING_APPROVAL) {
            throw new BusinessRuleViolationException("Only PENDING_APPROVAL accounts can be approved");
        }
        this.approvedBy = Objects.requireNonNull(managerId, "Manager ID cannot be null");
        this.status = AccountStatus.ACTIVE;
        this.rejectionReason = null;
    }

    public void reject(UUID managerId, String reason) {
        if (this.status != AccountStatus.PENDING_APPROVAL) {
            throw new BusinessRuleViolationException("Only PENDING_APPROVAL accounts can be rejected");
        }
        this.approvedBy = Objects.requireNonNull(managerId, "Manager ID cannot be null");
        this.status = AccountStatus.REJECTED;
        this.rejectionReason = Objects.requireNonNull(reason, "Rejection reason cannot be null");
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCpf() {
        return cpf;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
