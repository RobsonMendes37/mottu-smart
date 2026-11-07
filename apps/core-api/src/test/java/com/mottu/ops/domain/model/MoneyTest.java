package com.mottu.ops.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("Should create money and handle arithmetic, equality, hashCode, and toString with 100% line coverage")
    void shouldCreateMoneyAndOperate() {
        Money m1 = Money.of(10.0);
        Money m2 = Money.of(25.5);
        Money sum = m1.add(m2);

        assertThat(sum.getAmount()).isEqualByComparingTo("35.50");
        assertThat(Money.ZERO.getAmount()).isEqualByComparingTo("0.00");
        assertThat(Money.of(new BigDecimal("10.00"))).isEqualTo(m1);
        assertThat(m1).isEqualTo(Money.of(10.00));
        assertThat(m1).isNotEqualTo(m2);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo("other");
        assertThat(m1).isEqualTo(m1);
        assertThat(m1.hashCode()).isEqualTo(Money.of(10.00).hashCode());
        assertThat(m1.toString()).isEqualTo("10.00");

        assertThatThrownBy(() -> new Money(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Amount cannot be null");

        assertThatThrownBy(() -> m1.add(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Other money cannot be null");
    }
}
