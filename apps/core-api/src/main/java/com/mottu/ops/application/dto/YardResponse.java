package com.mottu.ops.application.dto;

import java.util.UUID;

public record YardResponse(
        UUID id,
        String name,
        String city,
        String address,
        boolean active
) {
}
