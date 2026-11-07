package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.RiderGarageResponse;

import java.util.UUID;

public interface GetRiderGarageUseCase {
    RiderGarageResponse getGarageForRider(UUID riderId);
}
