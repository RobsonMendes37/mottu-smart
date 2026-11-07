package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;

import java.util.Objects;

public record AiTriageDecision(
        InspectionStatus targetStatus,
        QueuePriorityReason priorityReason,
        Integer priorityWeight,
        boolean requiresHumanTriage
) {
    public static AiTriageDecision autoResolved() {
        return new AiTriageDecision(InspectionStatus.AUTO_RESOLVED, null, null, false);
    }

    public static AiTriageDecision invalidImage() {
        return new AiTriageDecision(InspectionStatus.INVALID_IMAGE, null, null, false);
    }

    public static AiTriageDecision humanTriage(QueuePriorityReason reason) {
        Objects.requireNonNull(reason, "Priority reason cannot be null for human triage");
        return new AiTriageDecision(InspectionStatus.HUMAN_TRIAGE, reason, reason.getWeight(), true);
    }

    public static AiTriageDecision awaitingRecovery() {
        return new AiTriageDecision(InspectionStatus.AWAITING_AI_RECOVERY, null, null, false);
    }
}
