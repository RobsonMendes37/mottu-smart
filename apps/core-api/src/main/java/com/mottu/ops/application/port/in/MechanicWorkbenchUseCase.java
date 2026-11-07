package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.MechanicInspectionDetailsResponse;
import com.mottu.ops.application.dto.MechanicQueueItemDto;
import com.mottu.ops.application.dto.PageResponse;
import com.mottu.ops.application.dto.ResolveInspectionRequest;
import com.mottu.ops.application.dto.ResolveInspectionResponse;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MechanicWorkbenchUseCase {

    PageResponse<MechanicQueueItemDto> getQueue(QueuePriorityReason reason, String plate, Pageable pageable);

    MechanicInspectionDetailsResponse getDetails(UUID inspectionId);

    ResolveInspectionResponse resolve(UUID mechanicId, UUID inspectionId, ResolveInspectionRequest request);
}
