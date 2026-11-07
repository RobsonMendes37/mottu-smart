package com.mottu.ops.application.port.out;

import com.mottu.ops.application.dto.RecentInspectionDto;

import java.util.List;
import java.util.UUID;

public interface InspectionRepositoryPort {
    int countOpenByContractId(UUID contractId);
    List<RecentInspectionDto> findRecentByContractId(UUID contractId, int limit);
    int countOpenByRiderId(UUID riderId);
    List<RecentInspectionDto> findRecentByRiderId(UUID riderId, int limit);
}
