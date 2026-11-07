package com.mottu.ops.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectUserRequestDto(
        @NotBlank(message = "Rejection reason cannot be blank")
        String reason
) {}
