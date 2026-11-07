package com.mottu.ops.application.dto;

import com.mottu.ops.domain.enums.UserRole;

public record RegisterCommand(
        String fullName,
        String cpf,
        String phone,
        String email,
        String password,
        UserRole role
) {}
