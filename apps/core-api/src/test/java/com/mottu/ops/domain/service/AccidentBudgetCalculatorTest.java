package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.VehicleDrivability;
import com.mottu.ops.domain.model.AccidentBudget;
import com.mottu.ops.domain.model.AccidentReportItem;
import com.mottu.ops.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccidentBudgetCalculatorTest {

    private AccidentBudgetCalculator calculator;
    private Money lightLaborFee;
    private Money heavyLaborFee;
    private Money towingFee;

    @BeforeEach
    void setUp() {
        calculator = new AccidentBudgetCalculator();
        lightLaborFee = Money.of(35.00);
        heavyLaborFee = Money.of(90.00);
        towingFee = Money.of(80.00);
    }

    @Test
    @DisplayName("UC-13.1: Orçamento com 0 peças e moto DRIVABLE deve resultar em R$ 0,00 total")
    void shouldCalculateZeroForEmptyPartsAndDrivable() {
        AccidentBudget budget = calculator.calculate(
                Collections.emptyList(),
                VehicleDrivability.DRIVABLE,
                lightLaborFee,
                heavyLaborFee,
                towingFee
        );

        assertThat(budget.partsSubtotal()).isEqualTo(Money.ZERO);
        assertThat(budget.laborFee()).isEqualTo(Money.ZERO);
        assertThat(budget.towingFee()).isEqualTo(Money.ZERO);
        assertThat(budget.totalEstimated()).isEqualTo(Money.ZERO);
        assertThat(budget.partsCount()).isZero();
    }

    @Test
    @DisplayName("UC-13.2: Orçamento com 2 peças e moto DRIVABLE deve cobrar peças + mão de obra leve (R$ 35,00) sem guincho")
    void shouldCalculateLightLaborForTwoPartsDrivable() {
        AccidentReportItem item1 = AccidentReportItem.createAiItem(
                UUID.randomUUID(), "Manete de Freio", Money.of(25.00), 1, 6
        );
        AccidentReportItem item2 = AccidentReportItem.createAiItem(
                UUID.randomUUID(), "Retrovisor Direito", Money.of(42.50), 1, 14
        );

        AccidentBudget budget = calculator.calculate(
                List.of(item1, item2),
                VehicleDrivability.DRIVABLE,
                lightLaborFee,
                heavyLaborFee,
                towingFee
        );

        assertThat(budget.partsSubtotal()).isEqualTo(Money.of(67.50));
        assertThat(budget.laborFee()).isEqualTo(Money.of(35.00));
        assertThat(budget.towingFee()).isEqualTo(Money.ZERO);
        assertThat(budget.totalEstimated()).isEqualTo(Money.of(102.50));
        assertThat(budget.partsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("UC-13.3: Orçamento com 3 peças e moto NEEDS_TOWING deve cobrar peças + mão de obra pesada (R$ 90,00) + guincho (R$ 80,00)")
    void shouldCalculateHeavyLaborAndTowingForThreePartsNeedingTowing() {
        AccidentReportItem item1 = AccidentReportItem.createAiItem(
                UUID.randomUUID(), "Manete de Freio", Money.of(25.00), 1, 6
        );
        AccidentReportItem item2 = AccidentReportItem.createAiItem(
                UUID.randomUUID(), "Retrovisor Direito", Money.of(42.50), 1, 14
        );
        AccidentReportItem item3 = AccidentReportItem.createAiItem(
                UUID.randomUUID(), "Pedal de Apoio", Money.of(80.00), 1, 21
        );

        AccidentBudget budget = calculator.calculate(
                List.of(item1, item2, item3),
                VehicleDrivability.NEEDS_TOWING,
                lightLaborFee,
                heavyLaborFee,
                towingFee
        );

        // Subtotal = 25.00 + 42.50 + 80.00 = 147.50
        // Mão de obra pesada (>= 3 peças) = 90.00
        // Guincho = 80.00
        // Total = 147.50 + 90.00 + 80.00 = 317.50
        assertThat(budget.partsSubtotal()).isEqualTo(Money.of(147.50));
        assertThat(budget.laborFee()).isEqualTo(Money.of(90.00));
        assertThat(budget.towingFee()).isEqualTo(Money.of(80.00));
        assertThat(budget.totalEstimated()).isEqualTo(Money.of(317.50));
        assertThat(budget.partsCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("UC-13.4: Deve calcular quantidade maior que 1 para itens de mesma peça")
    void shouldHandleQuantitiesProperly() {
        AccidentReportItem item = AccidentReportItem.createAiItem(
                UUID.randomUUID(), "Espelho Retrovisor", Money.of(40.00), 2, 8
        );

        AccidentBudget budget = calculator.calculate(
                List.of(item),
                VehicleDrivability.DRIVABLE,
                lightLaborFee,
                heavyLaborFee,
                towingFee
        );

        assertThat(budget.partsSubtotal()).isEqualTo(Money.of(80.00));
        assertThat(budget.laborFee()).isEqualTo(Money.of(35.00));
        assertThat(budget.totalEstimated()).isEqualTo(Money.of(115.00));
        assertThat(budget.partsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("UC-13.5: Deve usar taxas padrão quando valores nulos forem passados")
    void shouldUseDefaultFeesWhenNullPassed() {
        AccidentReportItem item = AccidentReportItem.createAiItem(
                UUID.randomUUID(), "Manete", Money.of(30.00), 1, 5
        );

        AccidentBudget budget = calculator.calculate(
                List.of(item),
                VehicleDrivability.NEEDS_TOWING,
                null,
                null,
                null
        );

        assertThat(budget.laborFee()).isEqualTo(Money.of(35.00));
        assertThat(budget.towingFee()).isEqualTo(Money.of(80.00));
        assertThat(budget.totalEstimated()).isEqualTo(Money.of(145.00));
    }

    @Test
    @DisplayName("UC-13.6: Validação de argumentos nulos obrigatórios")
    void shouldValidateNullArguments() {
        assertThatThrownBy(() -> calculator.calculate(null, VehicleDrivability.DRIVABLE, lightLaborFee, heavyLaborFee, towingFee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Items cannot be null");

        assertThatThrownBy(() -> calculator.calculate(Collections.emptyList(), null, lightLaborFee, heavyLaborFee, towingFee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Drivability cannot be null");
    }
}

