package com.mottu.ops.domain.service;

import com.mottu.ops.domain.enums.AccidentReportStatus;
import com.mottu.ops.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccidentStateMachineTest {

    private AccidentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new AccidentStateMachine();
    }

    @Test
    @DisplayName("UC-14.1: Transições legais devem ser permitidas com sucesso")
    void shouldAllowLegalTransitions() {
        // QUEUED_AI -> ESTIMATED_PENDING_WORKSHOP
        assertThatCode(() -> stateMachine.validateTransition(
                AccidentReportStatus.QUEUED_AI, AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP
        )).doesNotThrowAnyException();

        // QUEUED_AI -> AWAITING_AI_RECOVERY
        assertThatCode(() -> stateMachine.validateTransition(
                AccidentReportStatus.QUEUED_AI, AccidentReportStatus.AWAITING_AI_RECOVERY
        )).doesNotThrowAnyException();

        // AWAITING_AI_RECOVERY -> ESTIMATED_PENDING_WORKSHOP
        assertThatCode(() -> stateMachine.validateTransition(
                AccidentReportStatus.AWAITING_AI_RECOVERY, AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP
        )).doesNotThrowAnyException();

        // ESTIMATED_PENDING_WORKSHOP -> WORKSHOP_CONFIRMED
        assertThatCode(() -> stateMachine.validateTransition(
                AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP, AccidentReportStatus.WORKSHOP_CONFIRMED
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UC-14.2: Transições ilegais devem lançar InvalidStateTransitionException")
    void shouldRejectIllegalTransitions() {
        // QUEUED_AI -> WORKSHOP_CONFIRMED (não pode pular estimativa)
        assertThatThrownBy(() -> stateMachine.validateTransition(
                AccidentReportStatus.QUEUED_AI, AccidentReportStatus.WORKSHOP_CONFIRMED
        )).isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Illegal accident state transition");

        // WORKSHOP_CONFIRMED -> QUEUED_AI (estado terminal não pode retroceder)
        assertThatThrownBy(() -> stateMachine.validateTransition(
                AccidentReportStatus.WORKSHOP_CONFIRMED, AccidentReportStatus.QUEUED_AI
        )).isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Illegal accident state transition");

        // WORKSHOP_CONFIRMED -> ESTIMATED_PENDING_WORKSHOP
        assertThatThrownBy(() -> stateMachine.validateTransition(
                AccidentReportStatus.WORKSHOP_CONFIRMED, AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP
        )).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("UC-14.3: Validação de null")
    void shouldRejectNullStates() {
        assertThatThrownBy(() -> stateMachine.validateTransition(null, AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stateMachine.validateTransition(AccidentReportStatus.QUEUED_AI, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UC-14.4: Identificação de estado final")
    void shouldIdentifyTerminalState() {
        assertThat(stateMachine.isTerminal(AccidentReportStatus.WORKSHOP_CONFIRMED)).isTrue();
        assertThat(stateMachine.isTerminal(AccidentReportStatus.QUEUED_AI)).isFalse();
        assertThat(stateMachine.isTerminal(AccidentReportStatus.ESTIMATED_PENDING_WORKSHOP)).isFalse();
        assertThat(stateMachine.isTerminal(AccidentReportStatus.AWAITING_AI_RECOVERY)).isFalse();
    }
}

