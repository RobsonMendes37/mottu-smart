package com.mottu.ops.infrastructure.adapter.in.web.dto;

import com.mottu.ops.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequestDto(
        @NotBlank(message = "Full name cannot be blank")
        String fullName,

        @NotBlank(message = "CPF cannot be blank")
        @Pattern(regexp = "\\d{11}", message = "CPF must contain exactly 11 digits")
        String cpf,

        @NotBlank(message = "Phone cannot be blank")
        String phone,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password cannot be blank")
        String password,

        @NotNull(message = "Role is required")
        UserRole role
) {}
