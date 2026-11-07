package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectionStateMachineTest {

    private InspectionStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new InspectionStateMachine();
    }

    @Test
    @DisplayName("TC-FSM-01: QUEUED_AI -> AUTO_RESOLVED valid transition")
    void shouldTransitionQueuedAiToAutoResolved() {
        assertTrue(stateMachine.canTransition(InspectionStatus.QUEUED_AI, InspectionStatus.AUTO_RESOLVED));
        assertEquals(InspectionStatus.AUTO_RESOLVED,
                stateMachine.transition(InspectionStatus.QUEUED_AI, InspectionStatus.AUTO_RESOLVED));
    }

    @Test
    @DisplayName("TC-FSM-02: QUEUED_AI -> INVALID_IMAGE valid transition")
    void shouldTransitionQueuedAiToInvalidImage() {
        assertTrue(stateMachine.canTransition(InspectionStatus.QUEUED_AI, InspectionStatus.INVALID_IMAGE));
        assertEquals(InspectionStatus.INVALID_IMAGE,
                stateMachine.transition(InspectionStatus.QUEUED_AI, InspectionStatus.INVALID_IMAGE));
    }

    @Test
    @DisplayName("TC-FSM-03: QUEUED_AI -> HUMAN_TRIAGE valid transition")
    void shouldTransitionQueuedAiToHumanTriage() {
        assertTrue(stateMachine.canTransition(InspectionStatus.QUEUED_AI, InspectionStatus.HUMAN_TRIAGE));
        assertEquals(InspectionStatus.HUMAN_TRIAGE,
                stateMachine.transition(InspectionStatus.QUEUED_AI, InspectionStatus.HUMAN_TRIAGE));
    }

    @Test
    @DisplayName("TC-FSM-04: QUEUED_AI -> AWAITING_AI_RECOVERY valid transition")
    void shouldTransitionQueuedAiToAwaitingRecovery() {
        assertTrue(stateMachine.canTransition(InspectionStatus.QUEUED_AI, InspectionStatus.AWAITING_AI_RECOVERY));
        assertEquals(InspectionStatus.AWAITING_AI_RECOVERY,
                stateMachine.transition(InspectionStatus.QUEUED_AI, InspectionStatus.AWAITING_AI_RECOVERY));
    }

    @Test
    @DisplayName("TC-FSM-05: AWAITING_AI_RECOVERY transitions to retry QUEUED_AI, AUTO_RESOLVED, INVALID_IMAGE, or HUMAN_TRIAGE")
    void shouldTransitionAwaitingRecovery() {
        assertTrue(stateMachine.canTransition(InspectionStatus.AWAITING_AI_RECOVERY, InspectionStatus.QUEUED_AI));
        assertTrue(stateMachine.canTransition(InspectionStatus.AWAITING_AI_RECOVERY, InspectionStatus.AUTO_RESOLVED));
        assertTrue(stateMachine.canTransition(InspectionStatus.AWAITING_AI_RECOVERY, InspectionStatus.INVALID_IMAGE));
        assertTrue(stateMachine.canTransition(InspectionStatus.AWAITING_AI_RECOVERY, InspectionStatus.HUMAN_TRIAGE));

        assertEquals(InspectionStatus.QUEUED_AI,
                stateMachine.transition(InspectionStatus.AWAITING_AI_RECOVERY, InspectionStatus.QUEUED_AI));
    }

    @Test
    @DisplayName("TC-FSM-06: HUMAN_TRIAGE -> HUMAN_RESOLVED valid transition")
    void shouldTransitionHumanTriageToHumanResolved() {
        assertTrue(stateMachine.canTransition(InspectionStatus.HUMAN_TRIAGE, InspectionStatus.HUMAN_RESOLVED));
        assertEquals(InspectionStatus.HUMAN_RESOLVED,
                stateMachine.transition(InspectionStatus.HUMAN_TRIAGE, InspectionStatus.HUMAN_RESOLVED));
    }

    @Test
    @DisplayName("TC-FSM-07 (UC-09): Contest on AUTO_RESOLVED with contestedOnce=false and reason >= 10 chars succeeds")
    void shouldTransitionForValidContestation() {
        InspectionStatus next = stateMachine.transitionForContestation(
                InspectionStatus.AUTO_RESOLVED,
                false,
                "Justificativa válida com mais de dez caracteres"
        );
        assertEquals(InspectionStatus.HUMAN_TRIAGE, next);
    }

    @Test
    @DisplayName("TC-FSM-08 (UC-09): Contest with reason < 10 chars ('não fui') throws DomainValidationException")
    void shouldThrowWhenContestationReasonTooShort() {
        assertThrows(DomainValidationException.class, () ->
                stateMachine.transitionForContestation(InspectionStatus.AUTO_RESOLVED, false, "não fui"));
        assertThrows(DomainValidationException.class, () ->
                stateMachine.transitionForContestation(InspectionStatus.AUTO_RESOLVED, false, null));
        assertThrows(DomainValidationException.class, () ->
                stateMachine.transitionForContestation(InspectionStatus.AUTO_RESOLVED, false, "   12345   "));
    }

    @Test
    @DisplayName("TC-FSM-09 (UC-09): Contest when already contestedOnce=true throws BusinessRuleViolationException")
    void shouldThrowWhenAlreadyContested() {
        assertThrows(BusinessRuleViolationException.class, () ->
                stateMachine.transitionForContestation(InspectionStatus.AUTO_RESOLVED, true, "Nova justificativa longa"));
    }

    @Test
    @DisplayName("TC-FSM-10: Contest when status is NOT AUTO_RESOLVED throws InvalidStateTransitionException")
    void shouldThrowWhenContestingOutsideAutoResolved() {
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transitionForContestation(InspectionStatus.HUMAN_RESOLVED, false, "Justificativa longa válida"));
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transitionForContestation(InspectionStatus.QUEUED_AI, false, "Justificativa longa válida"));
    }

    @Test
    @DisplayName("TC-FSM-11: INVALID_IMAGE is terminal and cannot transition to any status")
    void shouldBlockTransitionFromInvalidImage() {
        assertTrue(stateMachine.isTerminal(InspectionStatus.INVALID_IMAGE));
        assertFalse(stateMachine.isOpen(InspectionStatus.INVALID_IMAGE));
        assertFalse(stateMachine.canTransition(InspectionStatus.INVALID_IMAGE, InspectionStatus.AUTO_RESOLVED));

        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transition(InspectionStatus.INVALID_IMAGE, InspectionStatus.AUTO_RESOLVED));
    }

    @Test
    @DisplayName("TC-FSM-12: CLOSED is terminal and cannot transition to any status")
    void shouldBlockTransitionFromClosed() {
        assertTrue(stateMachine.isTerminal(InspectionStatus.CLOSED));
        assertFalse(stateMachine.isOpen(InspectionStatus.CLOSED));
        assertFalse(stateMachine.canTransition(InspectionStatus.CLOSED, InspectionStatus.HUMAN_TRIAGE));

        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transition(InspectionStatus.CLOSED, InspectionStatus.HUMAN_TRIAGE));
    }

    @Test
    @DisplayName("TC-FSM-13: Illegal transition (QUEUED_AI -> HUMAN_RESOLVED) throws InvalidStateTransitionException")
    void shouldBlockIllegalTransition() {
        assertFalse(stateMachine.canTransition(InspectionStatus.QUEUED_AI, InspectionStatus.HUMAN_RESOLVED));

        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.transition(InspectionStatus.QUEUED_AI, InspectionStatus.HUMAN_RESOLVED));
    }

    @Test
    @DisplayName("Null parameters check in state machine")
    void shouldHandleNullParameters() {
        assertFalse(stateMachine.canTransition(null, InspectionStatus.AUTO_RESOLVED));
        assertFalse(stateMachine.canTransition(InspectionStatus.QUEUED_AI, null));
        assertFalse(stateMachine.canTransition(null, null));
        assertFalse(stateMachine.isOpen(null));
        assertTrue(stateMachine.isOpen(InspectionStatus.QUEUED_AI));
        assertFalse(stateMachine.isTerminal(InspectionStatus.QUEUED_AI));

        assertThrows(NullPointerException.class, () ->
                stateMachine.transition(null, InspectionStatus.AUTO_RESOLVED));
        assertThrows(NullPointerException.class, () ->
                stateMachine.transition(InspectionStatus.QUEUED_AI, null));
    }

    @Test
    @DisplayName("Valid transitions for SCHEDULED and HUMAN_RESOLVED")
    void shouldHandleScheduledAndHumanResolvedTransitions() {
        // AUTO_RESOLVED -> SCHEDULED or CLOSED
        assertTrue(stateMachine.canTransition(InspectionStatus.AUTO_RESOLVED, InspectionStatus.SCHEDULED));
        assertTrue(stateMachine.canTransition(InspectionStatus.AUTO_RESOLVED, InspectionStatus.CLOSED));

        // HUMAN_RESOLVED -> SCHEDULED or CLOSED
        assertTrue(stateMachine.canTransition(InspectionStatus.HUMAN_RESOLVED, InspectionStatus.SCHEDULED));
        assertTrue(stateMachine.canTransition(InspectionStatus.HUMAN_RESOLVED, InspectionStatus.CLOSED));

        // SCHEDULED -> CLOSED
        assertTrue(stateMachine.canTransition(InspectionStatus.SCHEDULED, InspectionStatus.CLOSED));
    }
}
