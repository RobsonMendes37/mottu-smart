package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.dto.AnalyticsSummaryResponseDto;
import com.mottu.ops.application.port.in.ManagerAnalyticsUseCase;
import com.mottu.ops.infrastructure.adapter.in.web.dto.analytics.DetailedAnalyticsResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @GetMapping("/detailed")
    public ResponseEntity<DetailedAnalyticsResponseDto> getDetailed() {
        return ResponseEntity.ok(managerAnalyticsUseCase.getDetailedAnalytics());
    }

    @PostMapping("/ai-advisor")
    public ResponseEntity<Map<String, Object>> getAiAdvisor(@RequestParam(defaultValue = "ALL") String yardKey) {
        return ResponseEntity.ok(managerAnalyticsUseCase.getAiAdvisorAdvice(yardKey));
    }
}
