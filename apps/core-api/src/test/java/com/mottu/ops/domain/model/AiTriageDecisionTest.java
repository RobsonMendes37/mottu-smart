package com.mottu.ops.domain.model;

import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.QueuePriorityReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTriageDecisionTest {

    @Test
    @DisplayName("Should create AiTriageDecision using factory helpers")
    void shouldCreateDecisions() {
        AiTriageDecision auto = AiTriageDecision.autoResolved();
        assertEquals(InspectionStatus.AUTO_RESOLVED, auto.targetStatus());
        assertFalse(auto.requiresHumanTriage());
        assertNull(auto.priorityReason());
        assertNull(auto.priorityWeight());

        AiTriageDecision invalid = AiTriageDecision.invalidImage();
        assertEquals(InspectionStatus.INVALID_IMAGE, invalid.targetStatus());
        assertFalse(invalid.requiresHumanTriage());

        AiTriageDecision human = AiTriageDecision.humanTriage(QueuePriorityReason.FRAUD_SUSPICION);
        assertEquals(InspectionStatus.HUMAN_TRIAGE, human.targetStatus());
        assertTrue(human.requiresHumanTriage());
        assertEquals(QueuePriorityReason.FRAUD_SUSPICION, human.priorityReason());
        assertEquals(4, human.priorityWeight());

        AiTriageDecision recovery = AiTriageDecision.awaitingRecovery();
        assertEquals(InspectionStatus.AWAITING_AI_RECOVERY, recovery.targetStatus());
        assertFalse(recovery.requiresHumanTriage());

        assertThrows(NullPointerException.class, () -> AiTriageDecision.humanTriage(null));
    }
}
