package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.RecentInspectionDto;
import com.mottu.ops.application.dto.RiderGarageResponse;
import com.mottu.ops.application.port.out.InspectionRepositoryPort;
import com.mottu.ops.application.port.out.MotorcycleRepositoryPort;
import com.mottu.ops.application.port.out.RentalContractRepositoryPort;
import com.mottu.ops.application.port.out.YardRepositoryPort;
import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.model.Motorcycle;
import com.mottu.ops.domain.model.RentalContract;
import com.mottu.ops.domain.model.Yard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderGarageServiceTest {

    @Mock
    private RentalContractRepositoryPort rentalContractRepositoryPort;

    @Mock
    private MotorcycleRepositoryPort motorcycleRepositoryPort;

    @Mock
    private YardRepositoryPort yardRepositoryPort;

    @Mock
    private InspectionRepositoryPort inspectionRepositoryPort;

    @InjectMocks
    private RiderGarageService riderGarageService;

    @Test
    @DisplayName("getGarageForRider with null riderId returns empty garage")
    void getGarageNullRiderReturnsEmpty() {
        RiderGarageResponse response = riderGarageService.getGarageForRider(null);
        assertNull(response.activeContract());
        assertEquals(0, response.openInspectionsCount());
        assertTrue(response.recentInspections().isEmpty());
    }

    @Test
    @DisplayName("getGarageForRider with no active contract returns empty garage")
    void getGarageNoActiveContractReturnsEmpty() {
        UUID riderId = UUID.randomUUID();
        when(rentalContractRepositoryPort.findActiveByRiderId(riderId)).thenReturn(Optional.empty());

        RiderGarageResponse response = riderGarageService.getGarageForRider(riderId);

        assertNull(response.activeContract());
        assertEquals(0, response.openInspectionsCount());
        assertTrue(response.recentInspections().isEmpty());
    }

    @Test
    @DisplayName("getGarageForRider with active contract returns populated overview")
    void getGarageActiveContractReturnsPopulatedOverview() {
        UUID riderId = UUID.randomUUID();
        UUID motoId = UUID.randomUUID();
        UUID yardId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        RentalContract contract = new RentalContract(contractId, riderId, motoId, Instant.now().minusSeconds(3600), 1000, null, true);
        Motorcycle moto = new Motorcycle(motoId, "MOT1A23", MotorcycleModel.MOTTU_SPORT_110I, yardId, MotorcycleStatus.RENTED, 1050, Instant.now());
        Yard yard = new Yard(yardId, "Galpão Butantã", "São Paulo", "Av. Corifeu", true);

        RecentInspectionDto inspection = new RecentInspectionDto(
                UUID.randomUUID(),
                InspectionStatus.AUTO_RESOLVED,
                DamageVerdict.PRE_EXISTENTE,
                BigDecimal.ZERO,
                Instant.now()
        );

        when(rentalContractRepositoryPort.findActiveByRiderId(riderId)).thenReturn(Optional.of(contract));
        when(motorcycleRepositoryPort.findById(motoId)).thenReturn(Optional.of(moto));
        when(yardRepositoryPort.findById(yardId)).thenReturn(Optional.of(yard));
        when(inspectionRepositoryPort.countOpenByContractId(contractId)).thenReturn(1);
        when(inspectionRepositoryPort.findRecentByContractId(contractId, 5)).thenReturn(List.of(inspection));

        RiderGarageResponse response = riderGarageService.getGarageForRider(riderId);

        assertNotNull(response.activeContract());
        assertEquals(contractId, response.activeContract().contractId());
        assertEquals("MOT1A23", response.activeContract().motorcycle().plate());
        assertEquals("Galpão Butantã", response.activeContract().motorcycle().yardName());
        assertEquals(1, response.openInspectionsCount());
        assertEquals(1, response.recentInspections().size());
    }
}
