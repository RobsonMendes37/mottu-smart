package com.mottu.ops.application.usecase;

import com.mottu.ops.application.dto.AnalyticsSummaryResponseDto;
import com.mottu.ops.application.port.in.ManagerAnalyticsUseCase;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataMotorcycleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ManagerAnalyticsService implements ManagerAnalyticsUseCase {

    private final SpringDataMotorcycleRepository motorcycleRepository;
    private final SpringDataInspectionRepository inspectionRepository;
    private final String lookerStudioEmbedUrl;

    public ManagerAnalyticsService(
            SpringDataMotorcycleRepository motorcycleRepository,
            SpringDataInspectionRepository inspectionRepository,
            @Value("${mottu.looker.embed-url:https://lookerstudio.google.com/embed/reporting/mottu-smart-ops-bi}") String lookerStudioEmbedUrl
    ) {
        this.motorcycleRepository = motorcycleRepository;
        this.inspectionRepository = inspectionRepository;
        this.lookerStudioEmbedUrl = lookerStudioEmbedUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponseDto getSummary() {
        long activeMotorcyclesCount = motorcycleRepository.countByStatus(MotorcycleStatus.RENTED);

        long autoResolvedCount = inspectionRepository.countByStatus(InspectionStatus.AUTO_RESOLVED);
        long humanResolvedCount = inspectionRepository.countByStatus(InspectionStatus.HUMAN_RESOLVED);
        long scheduledCount = inspectionRepository.countByStatus(InspectionStatus.SCHEDULED);
        long closedCount = inspectionRepository.countByStatus(InspectionStatus.CLOSED);

        long totalResolved = autoResolvedCount + humanResolvedCount + scheduledCount + closedCount;
        double autoResolvedPercentage = 0.0;
        if (totalResolved > 0) {
            autoResolvedPercentage = BigDecimal.valueOf((double) autoResolvedCount * 100.0 / totalResolved)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        long currentHumanQueueSize = inspectionRepository.countByStatus(InspectionStatus.HUMAN_TRIAGE);

        // Saved mechanic hours: each auto-resolved inspection saves roughly 0.5 hours (30 min) of triage/evaluation time
        double estimatedMechanicHoursSaved = BigDecimal.valueOf(autoResolvedCount * 0.5)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new AnalyticsSummaryResponseDto(
                activeMotorcyclesCount,
                autoResolvedPercentage,
                currentHumanQueueSize,
                estimatedMechanicHoursSaved,
                lookerStudioEmbedUrl
        );
    }
}
