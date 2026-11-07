package com.mottu.ops.domain.exception;

import com.mottu.ops.domain.enums.InspectionStatus;

public class InvalidStateTransitionException extends BusinessRuleViolationException {

    private final InspectionStatus from;
    private final InspectionStatus to;

    public InvalidStateTransitionException(InspectionStatus from, InspectionStatus to) {
        super(String.format("Invalid state transition from %s to %s", from, to));
        this.from = from;
        this.to = to;
    }

    public InvalidStateTransitionException(InspectionStatus from, InspectionStatus to, String detail) {
        super(String.format("Invalid state transition from %s to %s: %s", from, to, detail));
        this.from = from;
        this.to = to;
    }

    public InspectionStatus getFrom() {
        return from;
    }

    public InspectionStatus getTo() {
        return to;
    }
}
