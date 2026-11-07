package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.port.in.RiderInspectionUseCase;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inspections")
public class InspectionLiveStatusController {

    private final RiderInspectionUseCase riderInspectionUseCase;

    public InspectionLiveStatusController(RiderInspectionUseCase riderInspectionUseCase) {
        this.riderInspectionUseCase = riderInspectionUseCase;
    }

    @GetMapping(value = "/{id}/live-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeLiveStatusAlias(@PathVariable UUID id) {
        return riderInspectionUseCase.subscribeLiveStatus(id);
    }
}
