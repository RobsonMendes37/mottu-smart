package com.mottu.ops.domain.enums;

public enum QueuePriorityReason {
    CRITICAL_SAFETY(1),
    RIDER_CONTESTATION(2),
    LOW_AI_CONFIDENCE(3),
    FRAUD_SUSPICION(4);

    private final int weight;

    QueuePriorityReason(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
