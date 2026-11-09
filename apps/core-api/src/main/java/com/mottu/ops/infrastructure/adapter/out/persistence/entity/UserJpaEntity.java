package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AccountStatus status;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "password_sha256", length = 64)
    private String passwordSha256;

    @Column(name = "pix_key", length = 150)
    private String pixKey;

    @Column(name = "preferred_unit", nullable = false, length = 100)
    private String preferredUnit = "Unidade Butantã (SP-04)";

    @Column(name = "cnh_status", nullable = false, length = 80)
    private String cnhStatus = "Categoria A • Válida até 08/2029";

    @Column(name = "employee_code", length = 30)
    private String employeeCode;

    @Column(name = "role_title", length = 80)
    private String roleTitle;

    @Column(name = "shift_label", length = 80)
    private String shiftLabel;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserJpaEntity() {
    }

    public UserJpaEntity(
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
        this.id = id;
        this.fullName = fullName;
        this.cpf = cpf;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.approvedBy = approvedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getPasswordSha256() {
        return passwordSha256;
    }

    public void setPasswordSha256(String passwordSha256) {
        this.passwordSha256 = passwordSha256;
    }

    public String getPixKey() {
        return pixKey;
    }

    public void setPixKey(String pixKey) {
        this.pixKey = pixKey;
    }

    public String getPreferredUnit() {
        return preferredUnit;
    }

    public void setPreferredUnit(String preferredUnit) {
        this.preferredUnit = preferredUnit;
    }

    public String getCnhStatus() {
        return cnhStatus;
    }

    public void setCnhStatus(String cnhStatus) {
        this.cnhStatus = cnhStatus;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public void setShiftLabel(String shiftLabel) {
        this.shiftLabel = shiftLabel;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
