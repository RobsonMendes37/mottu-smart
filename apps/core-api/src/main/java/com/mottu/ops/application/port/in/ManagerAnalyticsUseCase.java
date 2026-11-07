package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.AnalyticsSummaryResponseDto;

public interface ManagerAnalyticsUseCase {
    AnalyticsSummaryResponseDto getSummary();
}
