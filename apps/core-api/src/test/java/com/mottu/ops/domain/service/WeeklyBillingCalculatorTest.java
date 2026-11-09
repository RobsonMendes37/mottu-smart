package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.MotorcycleModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeeklyBillingCalculatorTest {

    private final WeeklyBillingCalculator calculator = new WeeklyBillingCalculator();

    @ParameterizedTest
    @CsvSource({
            "MOTTU_SPORT_110I, 196.00",
            "HONDA_POP_110I, 210.00",
            "TVS_RAIDER_125, 224.00",
            "TVS_APACHE_160, 238.00"
    })
    @DisplayName("resolveBaseWeeklyPlanBrl should return expected weekly rate per model")
    void shouldResolveBaseWeeklyPlanBrl(MotorcycleModel model, String expectedAmount) {
        BigDecimal plan = calculator.resolveBaseWeeklyPlanBrl(model);
        assertThat(plan).isEqualByComparingTo(new BigDecimal(expectedAmount));
    }

    @Test
    @DisplayName("resolveBaseWeeklyPlanBrl should throw IllegalArgumentException for null model")
    void shouldThrowWhenModelIsNull() {
        assertThatThrownBy(() -> calculator.resolveBaseWeeklyPlanBrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Modelo da motocicleta é obrigatório");
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0.00",
            "50, 0.00",
            "119, 0.00",
            "120, 40.00",
            "150, 40.00"
    })
    @DisplayName("calculateDeliveryGoalBonusBrl should award R$ 40,00 when weekly deliveries >= 120")
    void shouldCalculateDeliveryBonus(int deliveries, String expectedBonus) {
        BigDecimal bonus = calculator.calculateDeliveryGoalBonusBrl(deliveries);
        assertThat(bonus).isEqualByComparingTo(new BigDecimal(expectedBonus));
    }

    @Test
    @DisplayName("calculateDeliveryGoalBonusBrl should throw IllegalArgumentException for negative deliveries")
    void shouldThrowForNegativeDeliveries() {
        assertThatThrownBy(() -> calculator.calculateDeliveryGoalBonusBrl(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contagem de entregas não pode ser negativa");
    }

    @Test
    @DisplayName("calculateNetWeeklyTotal should calculate net total without bonus when deliveries < 120")
    void shouldCalculateNetWeeklyTotalWithoutBonus() {
        BigDecimal net = calculator.calculateNetWeeklyTotal(
                new BigDecimal("196.00"),
                new BigDecimal("60.00"),
                new BigDecimal("15.00"),
                110
        );
        assertThat(net).isEqualByComparingTo(new BigDecimal("241.00"));
    }

    @Test
    @DisplayName("calculateNetWeeklyTotal should calculate net total subtracting R$ 40,00 bonus when deliveries >= 120")
    void shouldCalculateNetWeeklyTotalWithBonus() {
        BigDecimal net = calculator.calculateNetWeeklyTotal(
                new BigDecimal("196.00"),
                new BigDecimal("60.00"),
                new BigDecimal("15.00"),
                125
        );
        assertThat(net).isEqualByComparingTo(new BigDecimal("201.00"));
    }

    @Test
    @DisplayName("calculateNetWeeklyTotal should cap net total at 0.00 when deductions exceed plan + charges")
    void shouldCapNetWeeklyTotalAtZero() {
        BigDecimal net = calculator.calculateNetWeeklyTotal(
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                new BigDecimal("20.00"),
                130
        ); // 30 - 20 - 40 = -30 -> 0.00
        assertThat(net).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("calculateNetWeeklyTotal should throw IllegalArgumentException when any required parameter is null")
    void shouldThrowWhenParameterIsNull() {
        assertThatThrownBy(() -> calculator.calculateNetWeeklyTotal(null, BigDecimal.ZERO, BigDecimal.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateNetWeeklyTotal(BigDecimal.TEN, null, BigDecimal.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateNetWeeklyTotal(BigDecimal.TEN, BigDecimal.ZERO, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

