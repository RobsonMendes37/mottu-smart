package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.exception.DuplicateResourceException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.port.out.MotorcycleRepositoryPort;
import com.mottu.ops.application.port.out.RentalContractRepositoryPort;
import com.mottu.ops.application.port.out.UserRepositoryPort;
import com.mottu.ops.application.port.out.YardRepositoryPort;
import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.enums.UserRole;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.model.Motorcycle;
import com.mottu.ops.domain.model.RentalContract;
import com.mottu.ops.domain.model.User;
import com.mottu.ops.domain.model.Yard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FleetServiceTest {

    @Mock
    private MotorcycleRepositoryPort motorcycleRepositoryPort;

    @Mock
    private YardRepositoryPort yardRepositoryPort;

    @Mock
    private RentalContractRepositoryPort rentalContractRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private FleetService fleetService;

    @Test
    @DisplayName("listMotorcycles returns paginated PageResponse resolving active rider name")
    void listMotorcyclesReturnsPageResponse() {
        UUID motoId = UUID.randomUUID();
        UUID yardId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        Motorcycle moto = new Motorcycle(motoId, "MOT1A23", MotorcycleModel.MOTTU_SPORT_110I, yardId, MotorcycleStatus.RENTED, 500, Instant.now());
        Yard yard = new Yard(yardId, "Galpão Butantã", "São Paulo", "Av. Corifeu", true);
        RentalContract contract = new RentalContract(UUID.randomUUID(), riderId, motoId, Instant.now().minusSeconds(3600), 450, null, true);
        User rider = new User(riderId, "Carlos Entregador", "11122233344", "11999990000", "carlos@test.com", "hash", UserRole.RIDER, AccountStatus.ACTIVE, null, null, Instant.now());

        MotorcycleFilter filter = new MotorcycleFilter(null, null, null, null);
        when(motorcycleRepositoryPort.findAll(filter, 0, 20)).thenReturn(List.of(moto));
        when(motorcycleRepositoryPort.count(filter)).thenReturn(1L);
        when(yardRepositoryPort.findById(yardId)).thenReturn(Optional.of(yard));
        when(rentalContractRepositoryPort.findActiveByMotorcycleId(motoId)).thenReturn(Optional.of(contract));
        when(userRepositoryPort.findById(riderId)).thenReturn(Optional.of(rider));

        PageResponse<FleetMotorcycleResponse> response = fleetService.listMotorcycles(filter, 0, 20);

        assertEquals(1, response.items().size());
        assertEquals("MOT1A23", response.items().get(0).plate());
        assertEquals("Carlos Entregador", response.items().get(0).activeRiderName());
        assertEquals("Galpão Butantã", response.items().get(0).yardName());
        assertEquals(1L, response.totalElements());
    }

    @Test
    @DisplayName("createMotorcycle throws ResourceNotFoundException if yard does not exist")
    void createMotorcycleNonexistentYardThrows() {
        UUID yardId = UUID.randomUUID();
        CreateMotorcycleCommand command = new CreateMotorcycleCommand("TST9Z99", MotorcycleModel.MOTTU_SPORT_110I, yardId, 0);

        when(yardRepositoryPort.findById(yardId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fleetService.createMotorcycle(command));
    }

    @Test
    @DisplayName("createMotorcycle throws DuplicateResourceException if plate already exists")
    void createMotorcycleDuplicatePlateThrows() {
        UUID yardId = UUID.randomUUID();
        CreateMotorcycleCommand command = new CreateMotorcycleCommand("TST9Z99", MotorcycleModel.MOTTU_SPORT_110I, yardId, 0);
        Yard yard = new Yard(yardId, "Galpão Central", "São Paulo", "Rua A", true);

        when(yardRepositoryPort.findById(yardId)).thenReturn(Optional.of(yard));
        when(motorcycleRepositoryPort.existsByPlate("TST9Z99")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> fleetService.createMotorcycle(command));
    }

    @Test
    @DisplayName("createMotorcycle successfully persists AVAILABLE motorcycle")
    void createMotorcycleSuccess() {
        UUID yardId = UUID.randomUUID();
        CreateMotorcycleCommand command = new CreateMotorcycleCommand("TST9Z99", MotorcycleModel.MOTTU_SPORT_110I, yardId, 0);
        Yard yard = new Yard(yardId, "Galpão Central", "São Paulo", "Rua A", true);

        when(yardRepositoryPort.findById(yardId)).thenReturn(Optional.of(yard));
        when(motorcycleRepositoryPort.existsByPlate("TST9Z99")).thenReturn(false);
        when(motorcycleRepositoryPort.save(any(Motorcycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MotorcycleDetailsResponse response = fleetService.createMotorcycle(command);

        assertNotNull(response);
        assertEquals("TST9Z99", response.plate());
        assertEquals(MotorcycleStatus.AVAILABLE, response.status());
        assertEquals("Galpão Central", response.yardName());
    }

    @Test
    @DisplayName("getMotorcycleById throws ResourceNotFoundException if not found")
    void getByIdNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(motorcycleRepositoryPort.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fleetService.getMotorcycleById(id));
    }

    @Test
    @DisplayName("updateMotorcycleStatus rejects changing RENTED motorcycle to IN_MAINTENANCE directly")
    void updateStatusRentedMotorcycleThrows() {
        UUID id = UUID.randomUUID();
        Motorcycle moto = new Motorcycle(id, "MOT1A23", MotorcycleModel.MOTTU_SPORT_110I, UUID.randomUUID(), MotorcycleStatus.RENTED, 1000, Instant.now());

        when(motorcycleRepositoryPort.findById(id)).thenReturn(Optional.of(moto));

        assertThrows(BusinessRuleViolationException.class, () ->
                fleetService.updateMotorcycleStatus(id, MotorcycleStatus.IN_MAINTENANCE));
    }

    @Test
    @DisplayName("updateMotorcycleStatus transitions AVAILABLE to IN_MAINTENANCE successfully")
    void updateStatusAvailableToMaintenanceSuccess() {
        UUID id = UUID.randomUUID();
        UUID yardId = UUID.randomUUID();
        Motorcycle moto = new Motorcycle(id, "MOT1A23", MotorcycleModel.MOTTU_SPORT_110I, yardId, MotorcycleStatus.AVAILABLE, 1000, Instant.now());
        Yard yard = new Yard(yardId, "Galpão Central", "São Paulo", "Rua A", true);

        when(motorcycleRepositoryPort.findById(id)).thenReturn(Optional.of(moto));
        when(motorcycleRepositoryPort.save(any(Motorcycle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(yardRepositoryPort.findById(yardId)).thenReturn(Optional.of(yard));

        MotorcycleDetailsResponse response = fleetService.updateMotorcycleStatus(id, MotorcycleStatus.IN_MAINTENANCE);

        assertEquals(MotorcycleStatus.IN_MAINTENANCE, response.status());
    }
}
