package com.mottu.ops.domain.exception;

import com.mottu.ops.domain.enums.InspectionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvalidStateTransitionExceptionTest {

    @Test
    @DisplayName("Should create InvalidStateTransitionException with and without detail message")
    void shouldCreateException() {
        InvalidStateTransitionException ex1 = new InvalidStateTransitionException(
                InspectionStatus.QUEUED_AI,
                InspectionStatus.HUMAN_RESOLVED
        );
        assertEquals(InspectionStatus.QUEUED_AI, ex1.getFrom());
        assertEquals(InspectionStatus.HUMAN_RESOLVED, ex1.getTo());
        assertTrue(ex1.getMessage().contains("QUEUED_AI to HUMAN_RESOLVED"));

        InvalidStateTransitionException ex2 = new InvalidStateTransitionException(
                InspectionStatus.CLOSED,
                InspectionStatus.HUMAN_TRIAGE,
                "Closed tickets cannot be reopened"
        );
        assertEquals(InspectionStatus.CLOSED, ex2.getFrom());
        assertEquals(InspectionStatus.HUMAN_TRIAGE, ex2.getTo());
        assertTrue(ex2.getMessage().contains("Closed tickets cannot be reopened"));
    }
}
