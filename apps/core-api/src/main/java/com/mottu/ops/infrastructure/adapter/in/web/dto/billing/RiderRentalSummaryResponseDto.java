package com.mottu.ops.infrastructure.adapter.in.web.dto.billing;

import java.math.BigDecimal;
import java.util.List;

public record RiderRentalSummaryResponseDto(
        String contractCode,
        String activePlate,
        String model,
        BigDecimal baseWeeklyPlanBrl,
        BigDecimal punctualityDiscountBrl,
        BigDecimal deliveryBonusBrl,
        BigDecimal inspectionChargesBrl,
        BigDecimal weeklyTotalBrl,
        String paymentStatus,
        String dueDateLabel,
        List<RiderChargedInspectionDto> chargedInspections,
        int weeklyDeliveriesTotal,
        BigDecimal weeklyDeliveryEarningsBrl,
        int deliveryGoalTarget,
        List<RiderDailyDeliveryItemDto> deliveryDays
) {}

