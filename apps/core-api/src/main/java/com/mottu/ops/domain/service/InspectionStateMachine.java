package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.exception.InvalidStateTransitionException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class InspectionStateMachine {

    private static final Map<InspectionStatus, Set<InspectionStatus>> VALID_TRANSITIONS = new EnumMap<>(InspectionStatus.class);

    static {
        // QUEUED_AI transitions
        VALID_TRANSITIONS.put(InspectionStatus.QUEUED_AI, EnumSet.of(
                InspectionStatus.INVALID_IMAGE,
                InspectionStatus.AWAITING_AI_RECOVERY,
                InspectionStatus.AUTO_RESOLVED,
                InspectionStatus.HUMAN_TRIAGE
        ));

        // AWAITING_AI_RECOVERY transitions
        VALID_TRANSITIONS.put(InspectionStatus.AWAITING_AI_RECOVERY, EnumSet.of(
                InspectionStatus.QUEUED_AI,
                InspectionStatus.AUTO_RESOLVED,
                InspectionStatus.INVALID_IMAGE,
                InspectionStatus.HUMAN_TRIAGE
        ));

        // AUTO_RESOLVED transitions
        VALID_TRANSITIONS.put(InspectionStatus.AUTO_RESOLVED, EnumSet.of(
                InspectionStatus.HUMAN_TRIAGE,
                InspectionStatus.SCHEDULED,
                InspectionStatus.CLOSED
        ));

        // HUMAN_TRIAGE transitions
        VALID_TRANSITIONS.put(InspectionStatus.HUMAN_TRIAGE, EnumSet.of(
                InspectionStatus.HUMAN_RESOLVED
        ));

        // HUMAN_RESOLVED transitions
        VALID_TRANSITIONS.put(InspectionStatus.HUMAN_RESOLVED, EnumSet.of(
                InspectionStatus.SCHEDULED,
                InspectionStatus.CLOSED
        ));

        // SCHEDULED transitions
        VALID_TRANSITIONS.put(InspectionStatus.SCHEDULED, EnumSet.of(
                InspectionStatus.CLOSED
        ));

        // Terminal states
        VALID_TRANSITIONS.put(InspectionStatus.INVALID_IMAGE, Collections.emptySet());
        VALID_TRANSITIONS.put(InspectionStatus.CLOSED, Collections.emptySet());
    }

    public boolean canTransition(InspectionStatus from, InspectionStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<InspectionStatus> allowed = VALID_TRANSITIONS.getOrDefault(from, Collections.emptySet());
        return allowed.contains(to);
    }

    public InspectionStatus transition(InspectionStatus current, InspectionStatus target) {
        Objects.requireNonNull(current, "Current status cannot be null");
        Objects.requireNonNull(target, "Target status cannot be null");

        if (!canTransition(current, target)) {
            throw new InvalidStateTransitionException(current, target);
        }
        return target;
    }

    public InspectionStatus transitionForContestation(InspectionStatus current, boolean contestedOnce, String reason) {
        if (contestedOnce) {
            throw new BusinessRuleViolationException("Inspection was already contested once");
        }
        if (current != InspectionStatus.AUTO_RESOLVED) {
            throw new InvalidStateTransitionException(current, InspectionStatus.HUMAN_TRIAGE,
                    "Contestation is only allowed when status is AUTO_RESOLVED");
        }
        if (reason == null || reason.trim().length() < 10) {
            throw new DomainValidationException("Contestation reason must contain at least 10 non-whitespace characters");
        }
        return transition(current, InspectionStatus.HUMAN_TRIAGE);
    }

    public boolean isTerminal(InspectionStatus status) {
        return status == InspectionStatus.INVALID_IMAGE || status == InspectionStatus.CLOSED;
    }

    public boolean isOpen(InspectionStatus status) {
        return status != null && status.isOpen();
    }
}
