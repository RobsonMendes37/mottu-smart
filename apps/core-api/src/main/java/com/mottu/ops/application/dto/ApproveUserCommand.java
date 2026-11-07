package com.mottu.ops.application.dto;

import java.util.UUID;

public record ApproveUserCommand(
        UUID userId,
        UUID managerId,
        UUID motorcycleId
) {}
