package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.ManagerSettingsDto;

public interface ManagerSettingsUseCase {
    ManagerSettingsDto getSettings();
    ManagerSettingsDto updateSettings(ManagerSettingsDto request);
}
