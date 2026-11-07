package com.mottu.ops.infrastructure.adapter.in.web.dto;

import java.util.UUID;

public record UserProfileResponseDto(
        UUID userId,
        String fullName,
        String email,
        String cpf,
        String phone,
        String role,
        String status
) {}
