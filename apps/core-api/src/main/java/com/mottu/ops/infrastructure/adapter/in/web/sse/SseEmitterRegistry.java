package com.mottu.ops.infrastructure.adapter.in.web.sse;

import com.mottu.ops.application.dto.SseStatusChangeEventDto;
import com.mottu.ops.domain.enums.InspectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);
    private static final Long DEFAULT_TIMEOUT_MS = 180_000L; // 3 minutes

    private final Map<UUID, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    public SseEmitter registerEmitter(UUID inspectionId, InspectionStatus currentStatus) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        emitterMap.computeIfAbsent(inspectionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(inspectionId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send initial connection event
        try {
            String stepLabel = resolveStepLabel(currentStatus);
            SseStatusChangeEventDto initialEvent = new SseStatusChangeEventDto(inspectionId, currentStatus, stepLabel);
            emitter.send(SseEmitter.event()
                    .name("status-change")
                    .data(initialEvent));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event for inspection {}", inspectionId, e);
            cleanup.run();
        }

        return emitter;
    }

    public void emitStatusChange(UUID inspectionId, InspectionStatus newStatus) {
        List<SseEmitter> emitters = emitterMap.get(inspectionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String stepLabel = resolveStepLabel(newStatus);
        SseStatusChangeEventDto eventDto = new SseStatusChangeEventDto(inspectionId, newStatus, stepLabel);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("status-change")
                        .data(eventDto));
            } catch (Exception e) {
                log.debug("Removing disconnected SSE emitter for inspection {}", inspectionId);
                removeEmitter(inspectionId, emitter);
            }
        }
    }

    private void removeEmitter(UUID inspectionId, SseEmitter emitter) {
        List<SseEmitter> emitters = emitterMap.get(inspectionId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emitterMap.remove(inspectionId);
            }
        }
    }

    public static String resolveStepLabel(InspectionStatus status) {
        if (status == null) {
            return "Status desconhecido.";
        }
        return switch (status) {
            case QUEUED_AI -> "Aguardando análise da IA...";
            case AWAITING_AI_RECOVERY -> "Aguardando recuperação da IA...";
            case INVALID_IMAGE -> "Foto inválida. Envie nova foto.";
            case AUTO_RESOLVED -> "Resultado pronto!";
            case HUMAN_TRIAGE -> "Encaminhado para a mesa do mecânico.";
            case HUMAN_RESOLVED -> "Laudo finalizado pelo mecânico.";
            case SCHEDULED -> "Visita ao galpão agendada.";
            case CLOSED -> "Vistoria concluída.";
        };
    }
}
