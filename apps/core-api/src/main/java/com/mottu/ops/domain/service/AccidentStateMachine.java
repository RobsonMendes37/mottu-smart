package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class AccidentStateMachine {

    private final Map<AccidentReportStatus, Set<AccidentReportStatus>> allowedTransitions = new EnumMap<>(AccidentReportStatus.class);

    public AccidentStateMachine() {
        allowedTransitions.put(AccidentReportStatus.QUEUED_AI, EnumSet.of(
                AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP,
                AccidentReportStatus.AWAITING_AI_RECOVERY
        ));

        allowedTransitions.put(AccidentReportStatus.AWAITING_AI_RECOVERY, EnumSet.of(
                AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP
        ));

        allowedTransitions.put(AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP, EnumSet.of(
                AccidentReportStatus.WORKSHOP_CONFIRMED
        ));

        allowedTransitions.put(AccidentReportStatus.WORKSHOP_CONFIRMED, EnumSet.noneOf(AccidentReportStatus.class));
    }

    public void validateTransition(AccidentReportStatus currentStatus, AccidentReportStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new IllegalArgumentException("States cannot be null");
        }

        Set<AccidentReportStatus> validTargets = allowedTransitions.getOrDefault(currentStatus, EnumSet.noneOf(AccidentReportStatus.class));
        if (!validTargets.contains(targetStatus)) {
            throw new InvalidStateTransitionException(
                    "Illegal accident state transition from " + currentStatus + " to " + targetStatus
            );
        }
    }

    public boolean isTerminal(AccidentReportStatus status) {
        return status == AccidentReportStatus.WORKSHOP_CONFIRMED;
    }
}

