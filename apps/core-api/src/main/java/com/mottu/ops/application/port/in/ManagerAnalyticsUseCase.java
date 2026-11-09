package com.mottu.ops.application.port.in;

import com.mottu.ops.application.dto.AnalyticsSummaryResponseDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.analytics.DetailedAnalyticsResponseDto;

import java.util.Map;

public interface ManagerAnalyticsUseCase {
    AnalyticsSummaryResponseDto getSummary();
    DetailedAnalyticsResponseDto getDetailedAnalytics();
    default Map<String, Object> getAiAdvisorAdvice(String yardKey) {
        return getAiAdvisorAdvice(yardKey, 7, "ALL", "NORMAL");
    }
    Map<String, Object> getAiAdvisorAdvice(String yardKey, int horizonDays, String systemCategory, String scenario);
    Map<String, Object> queryOperationalData(String query, String yardKey);
}
