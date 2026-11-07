package com.mottu.ops.domain.enums;

public enum InspectionStatus {
    QUEUED_AI,
    INVALID_IMAGE,
    AWAITING_AI_RECOVERY,
    AUTO_RESOLVED,
    HUMAN_TRIAGE,
    HUMAN_RESOLVED,
    SCHEDULED,
    CLOSED;

    public boolean isOpen() {
        return this == QUEUED_AI || this == AWAITING_AI_RECOVERY || this == HUMAN_TRIAGE;
    }
}
