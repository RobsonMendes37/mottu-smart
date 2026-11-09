package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.MotorcycleModel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class WeeklyBillingCalculator {

    private static final BigDecimal BONUS_GOAL_BRL = new BigDecimal("40.00");
    private static final int DELIVERY_GOAL_THRESHOLD = 120;

    public BigDecimal resolveBaseWeeklyPlanBrl(MotorcycleModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Modelo da motocicleta é obrigatório.");
        }
        return switch (model) {
            case MOTTU_SPORT_110I -> new BigDecimal("196.00");
            case HONDA_POP_110I -> new BigDecimal("210.00");
            case TVS_RAIDER_125 -> new BigDecimal("224.00");
            case TVS_APACHE_160 -> new BigDecimal("238.00");
        };
    }

    public BigDecimal calculateDeliveryGoalBonusBrl(int weeklyDeliveriesCount) {
        if (weeklyDeliveriesCount < 0) {
            throw new IllegalArgumentException("Contagem de entregas não pode ser negativa.");
        }
        if (weeklyDeliveriesCount >= DELIVERY_GOAL_THRESHOLD) {
            return BONUS_GOAL_BRL;
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetWeeklyTotal(BigDecimal basePlan, BigDecimal inspectionCharges, BigDecimal discount, int weeklyDeliveriesCount) {
        if (basePlan == null || inspectionCharges == null || discount == null) {
            throw new IllegalArgumentException("Valores de plano base, cobrança de vistorias e desconto não podem ser nulos.");
        }

        BigDecimal bonus = calculateDeliveryGoalBonusBrl(weeklyDeliveriesCount);
        BigDecimal net = basePlan.add(inspectionCharges).subtract(discount).subtract(bonus);

        if (net.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return net.setScale(2, RoundingMode.HALF_UP);
    }
}

