package com.mottu.ops.infrastructure.adapter.in.web.dto.billing;

import java.math.BigDecimal;
import java.util.UUID;

public record BillingContractSummaryDto(
        UUID contractId,
        String contractCode,
        String riderName,
        String plate,
        String model,
        BigDecimal weeklyPlanBrl,
        BigDecimal inspectionChargesBrl,
        BigDecimal discountBrl,
        BigDecimal deliveryBonusBrl,
        BigDecimal netTotalBrl,
        String paymentStatus,
        String dueDateLabel
) {}

