package com.mottu.ops.infrastructure.adapter.in.web.dto.mechanic;

import jakarta.validation.constraints.NotBlank;

public record CreateServiceOrderRequestDto(
        @NotBlank(message = "Placa da motocicleta é obrigatória")
        String plate,

        @NotBlank(message = "Baia de atendimento é obrigatória")
        String bay,

        @NotBlank(message = "Título do serviço é obrigatório")
        String serviceTitle
) {}

