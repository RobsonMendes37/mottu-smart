package com.mottu.ops.infrastructure.adapter.out.security;

import com.mottu.ops.domain.enums.UserRole;

import java.security.Principal;
import java.util.UUID;

public record AuthenticatedUserPrincipal(
        UUID userId,
        String email,
        UserRole role,
        String fullName
) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
