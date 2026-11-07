package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.LoginCommand;
import com.mottu.ops.application.dto.LoginResultDto;
import com.mottu.ops.application.dto.PublicConfigDto;
import com.mottu.ops.application.dto.RegisterCommand;
import com.mottu.ops.application.dto.RegisterResultDto;
import com.mottu.ops.application.dto.UserProfileDto;

import java.util.UUID;

public interface AuthUseCase {
    PublicConfigDto getPublicConfig();
    RegisterResultDto register(RegisterCommand command);
    LoginResultDto login(LoginCommand command);
    UserProfileDto getProfile(UUID userId);
}
