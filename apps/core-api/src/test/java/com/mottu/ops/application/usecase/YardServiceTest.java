package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.YardResponse;
import com.mottu.ops.application.port.out.YardRepositoryPort;
import com.mottu.ops.domain.model.Yard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YardServiceTest {

    @Mock
    private YardRepositoryPort yardRepositoryPort;

    @InjectMocks
    private YardService yardService;

    @Test
    @DisplayName("getAllYards returns active yards mapped to YardResponse")
    void getAllYardsReturnsMappedResponses() {
        Yard yard1 = new Yard(UUID.randomUUID(), "Galpão Butantã", "São Paulo", "Rua A", true);
        Yard yard2 = new Yard(UUID.randomUUID(), "Galpão Guarulhos", "Guarulhos", "Rua B", true);

        when(yardRepositoryPort.findAllActive()).thenReturn(List.of(yard1, yard2));

        List<YardResponse> responses = yardService.getAllYards();

        assertEquals(2, responses.size());
        assertEquals(yard1.getName(), responses.get(0).name());
        assertEquals(yard2.getName(), responses.get(1).name());
    }
}
