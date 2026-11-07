package com.mottu.ops.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "system_settings")
public class SystemSettingsJpaEntity {

    @Id
    @Column(name = "setting_key", nullable = false, length = 60)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 100)
    private String settingValue;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SystemSettingsJpaEntity() {
    }

    public SystemSettingsJpaEntity(String settingKey, String settingValue, Instant updatedAt) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
