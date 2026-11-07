package com.mottu.ops.application.dto;

public record LoginCommand(
        String email,
        String password
) {}
