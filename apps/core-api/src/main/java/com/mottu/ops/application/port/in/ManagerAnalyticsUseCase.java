package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.AnalyticsSummaryResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.analytics.DetailedAnalyticsResponseDto;

import java.util.Map;

public interface ManagerAnalyticsUseCase {
    AnalyticsSummaryResponseDto getSummary();
    DetailedAnalyticsResponseDto getDetailedAnalytics();
    Map<String, Object> getAiAdvisorAdvice(String yardKey);
    Map<String, Object> queryOperationalData(String query, String yardKey);
}
