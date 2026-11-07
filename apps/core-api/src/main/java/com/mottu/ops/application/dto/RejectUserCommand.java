package com.mottu.ops.application.dto;

import java.util.UUID;

public record RejectUserCommand(
        UUID userId,
        UUID managerId,
        String reason
) {}
