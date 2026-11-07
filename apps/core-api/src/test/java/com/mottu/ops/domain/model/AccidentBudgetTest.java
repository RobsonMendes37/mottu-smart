package com.mottu.ops.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccidentBudgetTest {

    @Test
    @DisplayName("Deve criar orçamento imutável corretamente")
    void shouldCreateBudget() {
        AccidentBudget budget = new AccidentBudget(
                Money.of(100.00),
                Money.of(35.00),
                Money.of(80.00),
                Money.of(215.00),
                2
        );

        assertThat(budget.partsSubtotal()).isEqualTo(Money.of(100.00));
        assertThat(budget.laborFee()).isEqualTo(Money.of(35.00));
        assertThat(budget.towingFee()).isEqualTo(Money.of(80.00));
        assertThat(budget.totalEstimated()).isEqualTo(Money.of(215.00));
        assertThat(budget.partsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve criar orçamento zero")
    void shouldCreateZeroBudget() {
        AccidentBudget zero = AccidentBudget.zero();
        assertThat(zero.partsSubtotal()).isEqualTo(Money.ZERO);
        assertThat(zero.laborFee()).isEqualTo(Money.ZERO);
        assertThat(zero.towingFee()).isEqualTo(Money.ZERO);
        assertThat(zero.totalEstimated()).isEqualTo(Money.ZERO);
        assertThat(zero.partsCount()).isZero();
    }

    @Test
    @DisplayName("Validação de nulos")
    void shouldValidateNulls() {
        assertThatThrownBy(() -> new AccidentBudget(null, Money.ZERO, Money.ZERO, Money.ZERO, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("partsSubtotal cannot be null");
    }
}

