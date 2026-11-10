package com.mottu.ops.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Baseline360PhotosTest {

    @Test
    @DisplayName("Deve validar criação de vistoria 360 com 4 ângulos e foto do odômetro")
    void shouldValidateCompleteBaseline360() {
        Baseline360Photos photos = new Baseline360Photos(
                "https://storage.mottu.com/baseline/front.jpg",
                "https://storage.mottu.com/baseline/rear.jpg",
                "https://storage.mottu.com/baseline/right.jpg",
                "https://storage.mottu.com/baseline/left.jpg",
                "https://storage.mottu.com/baseline/odometer.jpg",
                12500,
                Instant.now()
        );

        assertThat(photos.isComplete()).isTrue();
        assertThat(photos.initialOdometerKm()).isEqualTo(12500);
    }

    @Test
    @DisplayName("Deve calcular escudo de proteção 48h / 100km corretamente")
    void shouldCalculateRemainingShield() {
        Instant pickupAt = Instant.now().minus(10, ChronoUnit.HOURS);
        int initialKm = 10000;
        int currentKm = 10040;

        BaselineShieldStatus shield = BaselineShieldStatus.calculate(pickupAt, initialKm, currentKm, Instant.now());

        assertThat(shield.isShieldActive()).isTrue();
        assertThat(shield.remainingHours()).isLessThanOrEqualTo(38.0);
        assertThat(shield.remainingKm()).isEqualTo(60);
    }

    @Test
    @DisplayName("Escudo deve expirar se rodar mais de 100km ou passar de 48h")
    void shouldExpireShieldWhenExceeded() {
        Instant pickupAt = Instant.now().minus(50, ChronoUnit.HOURS);
        int initialKm = 10000;
        int currentKm = 10020;

        BaselineShieldStatus shieldHoursExceeded = BaselineShieldStatus.calculate(pickupAt, initialKm, currentKm, Instant.now());
        assertThat(shieldHoursExceeded.isShieldActive()).isFalse();

        Instant recentPickup = Instant.now().minus(2, ChronoUnit.HOURS);
        int highKm = 10150; // +150km
        BaselineShieldStatus shieldKmExceeded = BaselineShieldStatus.calculate(recentPickup, initialKm, highKm, Instant.now());
        assertThat(shieldKmExceeded.isShieldActive()).isFalse();
    }

    @Test
    @DisplayName("Deve lançar exceção se fotos obrigatórias forem nulas ou vazias")
    void shouldThrowWhenAnglesMissing() {
        assertThatThrownBy(() -> new Baseline360Photos(
                null, "rear.jpg", "right.jpg", "left.jpg", "odo.jpg", 100, Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

