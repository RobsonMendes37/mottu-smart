package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.enums.ServiceOrderStatus;

public class ServiceOrderPolicy {

    public ServiceOrderStatus nextStatus(ServiceOrderStatus current) {
        if (current == null) {
            throw new IllegalArgumentException("Status atual da ordem de serviço não pode ser nulo.");
        }
        return switch (current) {
            case WAITING_BAY -> ServiceOrderStatus.IN_REPAIR;
            case IN_REPAIR -> ServiceOrderStatus.READY_PICKUP;
            case READY_PICKUP -> ServiceOrderStatus.READY_PICKUP;
        };
    }

    public MotorcycleStatus resolveMotorcycleStatusOnOrderTransition(ServiceOrderStatus newOrderStatus, boolean hasActiveRentalContract) {
        if (newOrderStatus == null) {
            throw new IllegalArgumentException("Status da ordem de serviço não pode ser nulo.");
        }
        return switch (newOrderStatus) {
            case IN_REPAIR -> MotorcycleStatus.IN_MAINTENANCE;
            case READY_PICKUP, WAITING_BAY -> hasActiveRentalContract ? MotorcycleStatus.RENTED : MotorcycleStatus.AVAILABLE;
        };
    }

    public int calculateUpdatedStock(int currentStock, int delta) {
        return Math.max(0, currentStock + delta);
    }
}

