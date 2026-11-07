package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.ContestInspectionRequest;
import com.mottu.ops.application.dto.ContestInspectionResponse;
import com.mottu.ops.application.dto.CreateAppointmentRequest;
import com.mottu.ops.application.dto.CreateAppointmentResponse;
import com.mottu.ops.application.dto.CreateInspectionRequest;
import com.mottu.ops.application.dto.CreateInspectionResponse;
import com.mottu.ops.application.dto.RiderInspectionDetailsResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface RiderInspectionUseCase {

    CreateInspectionResponse createInspection(UUID riderId, String idempotencyKey, CreateInspectionRequest request);

    RiderInspectionDetailsResponse getInspectionDetails(UUID riderId, UUID inspectionId);

    ContestInspectionResponse contestInspection(UUID riderId, UUID inspectionId, ContestInspectionRequest request);

    CreateAppointmentResponse createAppointment(UUID riderId, UUID inspectionId, CreateAppointmentRequest request);

    SseEmitter subscribeLiveStatus(UUID inspectionId);
}
