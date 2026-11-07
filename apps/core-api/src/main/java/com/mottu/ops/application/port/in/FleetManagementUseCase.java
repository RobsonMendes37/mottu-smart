package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.CreateMotorcycleCommand;
import com.mottu.ops.application.dto.FleetMotorcycleResponse;
import com.mottu.ops.application.dto.MotorcycleDetailsResponse;
import com.mottu.ops.application.dto.MotorcycleFilter;
import com.mottu.ops.application.dto.PageResponse;
import com.mottu.ops.domain.enums.MotorcycleStatus;

import java.util.UUID;

public interface FleetManagementUseCase {
    PageResponse<FleetMotorcycleResponse> listMotorcycles(MotorcycleFilter filter, int page, int size);
    MotorcycleDetailsResponse createMotorcycle(CreateMotorcycleCommand command);
    MotorcycleDetailsResponse getMotorcycleById(UUID id);
    MotorcycleDetailsResponse updateMotorcycleStatus(UUID id, MotorcycleStatus status);
}
