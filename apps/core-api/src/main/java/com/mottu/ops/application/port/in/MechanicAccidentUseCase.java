package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.ConfirmAccidentReportCommand;
import com.mottu.ops.application.dto.ConfirmAccidentReportResponse;
import com.mottu.ops.application.dto.MechanicAccidentDetailsResponse;
import com.mottu.ops.application.dto.MechanicAccidentQueueItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MechanicAccidentUseCase {

    Page<MechanicAccidentQueueItemDto> getAccidentsQueue(Pageable pageable);

    MechanicAccidentDetailsResponse getAccidentDetails(UUID accidentReportId);

    ConfirmAccidentReportResponse confirmAccident(UUID mechanicId, UUID accidentReportId, ConfirmAccidentReportCommand command);
}

