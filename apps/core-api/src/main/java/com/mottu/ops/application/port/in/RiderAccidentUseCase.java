package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.AccidentPhotoUploadUrlRequest;
import com.mottu.ops.application.dto.AccidentPhotoUploadUrlResponse;
import com.mottu.ops.application.dto.AccidentVideoUploadUrlRequest;
import com.mottu.ops.application.dto.AccidentVideoUploadUrlResponse;
import com.mottu.ops.application.dto.CreateAccidentReportCommand;
import com.mottu.ops.application.dto.CreateAccidentReportResponse;
import com.mottu.ops.application.dto.RiderAccidentReportDetailsResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface RiderAccidentUseCase {

    AccidentVideoUploadUrlResponse generateVideoUploadUrl(AccidentVideoUploadUrlRequest request);

    AccidentPhotoUploadUrlResponse generatePhotoUploadUrl(AccidentPhotoUploadUrlRequest request);

    CreateAccidentReportResponse createAccidentReport(UUID riderId, String idempotencyKey, CreateAccidentReportCommand command);

    RiderAccidentReportDetailsResponse getAccidentDetails(UUID riderId, UUID accidentReportId);

    SseEmitter subscribeLiveStatus(UUID accidentReportId);
}

