package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.ManagerSettingsDto;
import com.mottu.ops.application.port.in.ManagerSettingsUseCase;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.SystemSettingsJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataSystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class ManagerSettingsService implements ManagerSettingsUseCase {

    private static final String STANDARD_LABOR_FEE_KEY = "STANDARD_LABOR_FEE_BRL";
    private static final BigDecimal DEFAULT_LABOR_FEE = new BigDecimal("35.00");

    private final SpringDataSystemSettingsRepository settingsRepository;

    public ManagerSettingsService(SpringDataSystemSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerSettingsDto getSettings() {
        BigDecimal fee = settingsRepository.findBySettingKey(STANDARD_LABOR_FEE_KEY)
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(DEFAULT_LABOR_FEE);
        return new ManagerSettingsDto(fee);
    }

    @Override
    @Transactional
    public ManagerSettingsDto updateSettings(ManagerSettingsDto request) {
        BigDecimal fee = (request != null && request.standardLaborFeeBrl() != null)
                ? request.standardLaborFeeBrl()
                : DEFAULT_LABOR_FEE;

        SystemSettingsJpaEntity entity = settingsRepository.findBySettingKey(STANDARD_LABOR_FEE_KEY)
                .orElseGet(() -> new SystemSettingsJpaEntity(STANDARD_LABOR_FEE_KEY, fee.toString(), Instant.now()));

        entity.setSettingValue(fee.toString());
        entity.setUpdatedAt(Instant.now());
        settingsRepository.save(entity);

        return new ManagerSettingsDto(fee);
    }
}
