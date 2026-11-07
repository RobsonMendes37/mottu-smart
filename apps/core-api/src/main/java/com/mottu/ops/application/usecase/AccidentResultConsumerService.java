package com.mottu.ops.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.AccidentDeduplicatedPartDto;
import com.mottu.ops.application.dto.AccidentResultPayloadDto;
import com.mottu.ops.application.port.in.AccidentResultConsumerUseCase;
import com.mottu.ops.application.port.out.AccidentReportRepositoryPort;
import com.mottu.ops.domain.model.AccidentReport;
import com.mottu.ops.domain.model.AccidentReportItem;
import com.mottu.ops.domain.model.Money;
import com.mottu.ops.domain.service.AccidentBudgetCalculator;
import com.mottu.ops.domain.service.AccidentStateMachine;
import com.mottu.ops.infrastructure.adapter.in.web.sse.SseEmitterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AccidentResultConsumerService implements AccidentResultConsumerUseCase {

    private static final Logger log = LoggerFactory.getLogger(AccidentResultConsumerService.class);
    public static final String ACCIDENT_RESULTS_QUEUE = "queue:ai_accident_results";

    private final AccidentReportRepositoryPort accidentReportRepository;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final AccidentBudgetCalculator budgetCalculator = new AccidentBudgetCalculator();
    private final AccidentStateMachine stateMachine = new AccidentStateMachine();

    public AccidentResultConsumerService(
            AccidentReportRepositoryPort accidentReportRepository,
            SseEmitterRegistry sseEmitterRegistry,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper
    ) {
        this.accidentReportRepository = accidentReportRepository;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void processResult(AccidentResultPayloadDto payload) {
        if (payload == null || payload.accidentReportId() == null) {
            return;
        }

        Optional<AccidentReport> opt = accidentReportRepository.findById(payload.accidentReportId());
        if (opt.isEmpty()) {
            log.warn("Received accident AI result for unknown report {}", payload.accidentReportId());
            return;
        }

        AccidentReport report = opt.get();

        if ("AI_UNAVAILABLE".equalsIgnoreCase(payload.outcome())) {
            log.warn("AI service unavailable for accident report {}, marking AWAITING_AI_RECOVERY", report.getId());
            report.markAwaitingAiRecovery(stateMachine);
            accidentReportRepository.save(report);
            sseEmitterRegistry.emitAccidentStatusChange(report.getId(), report.getStatus());
            return;
        }

        // Outcome SUCCESS
        List<AccidentReportItem> items = new ArrayList<>();
        if (payload.deduplicatedParts() != null) {
            for (AccidentDeduplicatedPartDto part : payload.deduplicatedParts()) {
                items.add(AccidentReportItem.createAiItem(
                        part.catalogPartId(),
                        part.partName(),
                        Money.of(part.unitPriceBrl()),
                        part.quantity(),
                        part.videoTimestampSeconds()
                ));
            }
        }

        double confidence = payload.confidence() != null ? payload.confidence() : 0.85;
        boolean structuralRisk = Boolean.TRUE.equals(payload.structuralRiskAlert());
        String technicalSummary = payload.aiTechnicalSummary() != null ? payload.aiTechnicalSummary() : "Análise automatizada concluída.";

        report.applyAiEstimation(confidence, structuralRisk, technicalSummary, items, budgetCalculator, stateMachine);
        accidentReportRepository.save(report);

        sseEmitterRegistry.emitAccidentStatusChange(report.getId(), report.getStatus());
        log.info("Accident report {} estimated successfully with {} parts, total {}", report.getId(), items.size(), report.getBudget().totalEstimated().getAmount());
    }

    @Scheduled(fixedDelay = 2000)
    public void pollResultsQueue() {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return;
            }
            String rawJson = redisTemplate.opsForList().rightPop(ACCIDENT_RESULTS_QUEUE);
            if (rawJson != null && !rawJson.isBlank()) {
                AccidentResultPayloadDto payload = objectMapper.readValue(rawJson, AccidentResultPayloadDto.class);
                processResult(payload);
            }
        } catch (Exception e) {
            log.debug("Error or timeout polling Redis accident results: {}", e.getMessage());
        }
    }
}

