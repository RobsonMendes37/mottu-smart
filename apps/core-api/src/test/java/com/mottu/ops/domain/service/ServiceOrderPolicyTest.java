package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.enums.ServiceOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceOrderPolicyTest {

    private final ServiceOrderPolicy policy = new ServiceOrderPolicy();

    @Test
    @DisplayName("nextStatus should transition WAITING_BAY to IN_REPAIR")
    void shouldTransitionWaitingBayToInRepair() {
        assertThat(policy.nextStatus(ServiceOrderStatus.WAITING_BAY))
                .isEqualTo(ServiceOrderStatus.IN_REPAIR);
    }

    @Test
    @DisplayName("nextStatus should transition IN_REPAIR to READY_PICKUP")
    void shouldTransitionInRepairToReadyPickup() {
        assertThat(policy.nextStatus(ServiceOrderStatus.IN_REPAIR))
                .isEqualTo(ServiceOrderStatus.READY_PICKUP);
    }

    @Test
    @DisplayName("nextStatus should remain READY_PICKUP idempotently")
    void shouldKeepReadyPickupIdempotent() {
        assertThat(policy.nextStatus(ServiceOrderStatus.READY_PICKUP))
                .isEqualTo(ServiceOrderStatus.READY_PICKUP);
    }

    @Test
    @DisplayName("nextStatus should throw IllegalArgumentException for null")
    void shouldThrowWhenNextStatusNull() {
        assertThatThrownBy(() -> policy.nextStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status atual da ordem de serviço não pode ser nulo");
    }

    @Test
    @DisplayName("resolveMotorcycleStatusOnOrderTransition should return IN_MAINTENANCE for IN_REPAIR")
    void shouldResolveInMaintenanceWhenInRepair() {
        assertThat(policy.resolveMotorcycleStatusOnOrderTransition(ServiceOrderStatus.IN_REPAIR, true))
                .isEqualTo(MotorcycleStatus.IN_MAINTENANCE);
        assertThat(policy.resolveMotorcycleStatusOnOrderTransition(ServiceOrderStatus.IN_REPAIR, false))
                .isEqualTo(MotorcycleStatus.IN_MAINTENANCE);
    }

    @Test
    @DisplayName("resolveMotorcycleStatusOnOrderTransition should resolve RENTED or AVAILABLE for READY_PICKUP")
    void shouldResolveRentedOrAvailableWhenReadyPickup() {
        assertThat(policy.resolveMotorcycleStatusOnOrderTransition(ServiceOrderStatus.READY_PICKUP, true))
                .isEqualTo(MotorcycleStatus.RENTED);
        assertThat(policy.resolveMotorcycleStatusOnOrderTransition(ServiceOrderStatus.READY_PICKUP, false))
                .isEqualTo(MotorcycleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("resolveMotorcycleStatusOnOrderTransition should resolve RENTED or AVAILABLE for WAITING_BAY")
    void shouldResolveRentedOrAvailableWhenWaitingBay() {
        assertThat(policy.resolveMotorcycleStatusOnOrderTransition(ServiceOrderStatus.WAITING_BAY, true))
                .isEqualTo(MotorcycleStatus.RENTED);
        assertThat(policy.resolveMotorcycleStatusOnOrderTransition(ServiceOrderStatus.WAITING_BAY, false))
                .isEqualTo(MotorcycleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("resolveMotorcycleStatusOnOrderTransition should throw IllegalArgumentException for null status")
    void shouldThrowWhenOrderStatusIsNull() {
        assertThatThrownBy(() -> policy.resolveMotorcycleStatusOnOrderTransition(null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status da ordem de serviço não pode ser nulo");
    }

    @ParameterizedTest
    @CsvSource({
            "10, -1, 9",
            "4, -5, 0",
            "0, -2, 0",
            "5, 10, 15",
            "0, 0, 0"
    })
    @DisplayName("calculateUpdatedStock should add delta and cap minimum at 0")
    void shouldCalculateUpdatedStockCorrectly(int currentStock, int delta, int expected) {
        assertThat(policy.calculateUpdatedStock(currentStock, delta)).isEqualTo(expected);
    }
}

