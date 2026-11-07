package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.AnalyticsSummaryResponseDto;
import com.mottu.ops.application.port.in.ManagerAnalyticsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/analytics")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerAnalyticsController {

    private final ManagerAnalyticsUseCase managerAnalyticsUseCase;

    public ManagerAnalyticsController(ManagerAnalyticsUseCase managerAnalyticsUseCase) {
        this.managerAnalyticsUseCase = managerAnalyticsUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponseDto> getSummary() {
        return ResponseEntity.ok(managerAnalyticsUseCase.getSummary());
    }
}
