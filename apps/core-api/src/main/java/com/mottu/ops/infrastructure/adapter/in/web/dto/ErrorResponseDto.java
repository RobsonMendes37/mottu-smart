package com.mottu.ops.infrastructure.adapter.in.web.dto;

public record ErrorResponseDto(
        String title,
        int status,
        String detail,
        String correlationId
) {}
