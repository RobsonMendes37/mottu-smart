package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.ContestInspectionRequest;
import com.mottu.ops.application.dto.MatchedPartDto;
import com.mottu.ops.application.dto.ResolveInspectionRequest;
import com.mottu.ops.application.dto.TriageResultPayloadDto;
import com.mottu.ops.application.port.in.AiTriageResultConsumerUseCase;
import com.mottu.ops.application.port.in.SlaRecoverySchedulerUseCase;
import com.mottu.ops.domain.enums.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MechanicWorkbenchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataInspectionRepository inspectionRepository;

    @Autowired
    private SpringDataInspectionItemRepository inspectionItemRepository;

    @Autowired
    private SpringDataInspectionAuditLogRepository auditLogRepository;

    @Autowired
    private SpringDataAnalyticsOutboxRepository outboxRepository;

    @Autowired
    private SpringDataCatalogPartRepository catalogPartRepository;

    @Autowired
    private SpringDataRentalContractRepository rentalContractRepository;

    @Autowired
    private SpringDataMotorcycleRepository motorcycleRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataYardRepository yardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AiTriageResultConsumerUseCase aiResultConsumer;

    @Autowired
    private SlaRecoverySchedulerUseCase slaRecoveryScheduler;

    private UserJpaEntity riderCarlos;
    private UserJpaEntity mechanicUser;
    private UserJpaEntity managerUser;
    private MotorcycleJpaEntity motoCarlos;
    private RentalContractJpaEntity contractCarlos;
    private CatalogPartJpaEntity partManete;
    private CatalogPartJpaEntity partLona;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        auditLogRepository.deleteAll();
        inspectionItemRepository.deleteAll();
        inspectionRepository.deleteAll();
        rentalContractRepository.deleteAll();
        motorcycleRepository.deleteAll();
        catalogPartRepository.deleteAll();
        userRepository.deleteAll();
        yardRepository.deleteAll();

        YardJpaEntity yard = yardRepository.save(new YardJpaEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Galpão Butantã", "São Paulo", "Av. Corifeu, 1000", true
        ));

        riderCarlos = new UserJpaEntity();
        riderCarlos.setId(UUID.fromString("e0000000-0000-0000-0000-000000000003"));
        riderCarlos.setFullName("Carlos Entregador");
        riderCarlos.setCpf("33344455566");
        riderCarlos.setPhone("11987654323");
        riderCarlos.setEmail("entregador@mottu-ops.local");
        riderCarlos.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        riderCarlos.setRole(UserRole.RIDER);
        riderCarlos.setStatus(AccountStatus.ACTIVE);
        riderCarlos.setCreatedAt(Instant.now());
        riderCarlos = userRepository.save(riderCarlos);

        mechanicUser = new UserJpaEntity();
        mechanicUser.setId(UUID.fromString("e0000000-0000-0000-0000-000000000002"));
        mechanicUser.setFullName("Mecânico Chefe");
        mechanicUser.setCpf("22233344455");
        mechanicUser.setPhone("11987654322");
        mechanicUser.setEmail("mecanico@mottu-ops.local");
        mechanicUser.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        mechanicUser.setRole(UserRole.MECHANIC);
        mechanicUser.setStatus(AccountStatus.ACTIVE);
        mechanicUser.setCreatedAt(Instant.now());
        mechanicUser = userRepository.save(mechanicUser);

        managerUser = new UserJpaEntity();
        managerUser.setId(UUID.fromString("e0000000-0000-0000-0000-000000000001"));
        managerUser.setFullName("Gestor Operações");
        managerUser.setCpf("11122233344");
        managerUser.setPhone("11987654321");
        managerUser.setEmail("gestor@mottu-ops.local");
        managerUser.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        managerUser.setRole(UserRole.MANAGER);
        managerUser.setStatus(AccountStatus.ACTIVE);
        managerUser.setCreatedAt(Instant.now());
        managerUser = userRepository.save(managerUser);

        motoCarlos = motorcycleRepository.save(new MotorcycleJpaEntity(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "MOT1A23",
                MotorcycleModel.MOTTU_SPORT_110I,
                yard.getId(),
                MotorcycleStatus.RENTED,
                1000,
                Instant.now().minusSeconds(3600 * 12)
        ));

        contractCarlos = rentalContractRepository.save(new RentalContractJpaEntity(
                UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                riderCarlos.getId(),
                motoCarlos.getId(),
                Instant.now().minusSeconds(3600 * 12),
                1000,
                null,
                true
        ));

        partManete = new CatalogPartJpaEntity();
        partManete.setId(UUID.fromString("77777777-7777-7777-7777-777777777771"));
        partManete.setVtexSku("SKU-MANETE-DIR-SPORT");
        partManete.setMotorcycleModel(MotorcycleModel.MOTTU_SPORT_110I);
        partManete.setPartName("Manete de Freio Direito");
        partManete.setCategory("Comandos & Freios");
        partManete.setUnitPriceBrl(BigDecimal.valueOf(25.00));
        partManete.setNaturalWear(false);
        partManete.setPriceOverridden(false);
        partManete.setUpdatedAt(Instant.now());
        partManete = catalogPartRepository.save(partManete);

        partLona = new CatalogPartJpaEntity();
        partLona.setId(UUID.fromString("77777777-7777-7777-7777-777777777772"));
        partLona.setVtexSku("SKU-LONA-FREIO-TRAS-SPORT");
        partLona.setMotorcycleModel(MotorcycleModel.MOTTU_SPORT_110I);
        partLona.setPartName("Lona de Freio Traseira");
        partLona.setCategory("Freios");
        partLona.setUnitPriceBrl(BigDecimal.valueOf(33.00));
        partLona.setNaturalWear(true);
        partLona.setPriceOverridden(false);
        partLona.setUpdatedAt(Instant.now());
        partLona = catalogPartRepository.save(partLona);
    }

    // =========================================================================
    // UC-10: Priority Ordered Mechanic Queue (1 -> 2 -> 3 -> 4) & RBAC
    // =========================================================================

    @Test
    @DisplayName("UC-10: GET /api/v1/mechanic/queue returns items strictly ordered by priority_weight ASC, created_at ASC")
    @WithMockUser(username = "mecanico@mottu-ops.local", roles = "MECHANIC")
    void shouldReturnMechanicQueueInStrictPriorityOrder() throws Exception {
        // Create 4 tickets in reverse order
        createTriageTicket("t-fraud", QueuePriorityReason.FRAUD_SUSPICION, 4, Instant.now().minusSeconds(400));
        createTriageTicket("t-low-ai", QueuePriorityReason.LOW_AI_CONFIDENCE, 3, Instant.now().minusSeconds(300));
        createTriageTicket("t-contest", QueuePriorityReason.RIDER_CONTESTATION, 2, Instant.now().minusSeconds(200));
        createTriageTicket("t-safety", QueuePriorityReason.CRITICAL_SAFETY, 1, Instant.now().minusSeconds(100));

        mockMvc.perform(get("/api/v1/mechanic/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].priorityReason").value("CRITICAL_SAFETY"))
                .andExpect(jsonPath("$.items[0].priorityWeight").value(1))
                .andExpect(jsonPath("$.items[1].priorityReason").value("RIDER_CONTESTATION"))
                .andExpect(jsonPath("$.items[1].priorityWeight").value(2))
                .andExpect(jsonPath("$.items[2].priorityReason").value("LOW_AI_CONFIDENCE"))
                .andExpect(jsonPath("$.items[2].priorityWeight").value(3))
                .andExpect(jsonPath("$.items[3].priorityReason").value("FRAUD_SUSPICION"))
                .andExpect(jsonPath("$.items[3].priorityWeight").value(4));
    }

    @Test
    @DisplayName("UC-10 RBAC: ROLE_RIDER is forbidden (403), ROLE_MECHANIC and ROLE_MANAGER have access (200)")
    void shouldEnforceRbacOnMechanicEndpoints() throws Exception {
        // RIDER -> 403
        mockMvc.perform(get("/api/v1/mechanic/queue")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("entregador@mottu-ops.local").roles("RIDER")))
                .andExpect(status().isForbidden());

        // MECHANIC -> 200
        mockMvc.perform(get("/api/v1/mechanic/queue")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("mecanico@mottu-ops.local").roles("MECHANIC")))
                .andExpect(status().isOk());

        // MANAGER -> 200
        mockMvc.perform(get("/api/v1/mechanic/queue")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("gestor@mottu-ops.local").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // UC-10: Decision Desk Detail & Resolution
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/mechanic/inspections/{id} returns full decision desk view")
    @WithMockUser(username = "mecanico@mottu-ops.local", roles = "MECHANIC")
    void shouldReturnMechanicInspectionDetails() throws Exception {
        InspectionJpaEntity ticket = createTriageTicket("t-detail-1", QueuePriorityReason.LOW_AI_CONFIDENCE, 3, Instant.now());
        ticket.setAiVerdict(DamageVerdict.MAU_USO);
        ticket.setAiConfidence(BigDecimal.valueOf(0.750));
        ticket.setAiTechnicalSummary("Possível avaria no manete.");
        inspectionRepository.save(ticket);

        mockMvc.perform(get("/api/v1/mechanic/inspections/" + ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.plate").value("MOT1A23"))
                .andExpect(jsonPath("$.motorcycleModel").value("MOTTU_SPORT_110I"))
                .andExpect(jsonPath("$.riderName").value("Carlos Entregador"))
                .andExpect(jsonPath("$.hoursSincePickup").value(closeTo(12.0, 1.0)))
                .andExpect(jsonPath("$.aiSuggestedVerdict").value("MAU_USO"))
                .andExpect(jsonPath("$.aiConfidence").value(0.75));
    }

    @Test
    @DisplayName("UC-10: POST /api/v1/mechanic/inspections/{id}/resolve recalculates budget, records audit log and outbox")
    @WithMockUser(username = "mecanico@mottu-ops.local", roles = "MECHANIC")
    void shouldResolveInspectionAndRecalculateBudget() throws Exception {
        InspectionJpaEntity ticket = createTriageTicket("t-resolve-1", QueuePriorityReason.LOW_AI_CONFIDENCE, 3, Instant.now());
        ticket.setAiVerdict(DamageVerdict.MAU_USO);
        ticket.setAiConfidence(BigDecimal.valueOf(0.750));
        inspectionRepository.save(ticket);

        // Mechanic overrules AI to DESGASTE_NATURAL
        ResolveInspectionRequest request = new ResolveInspectionRequest(
                DamageVerdict.DESGASTE_NATURAL,
                List.of(partLona.getId()),
                "Desgaste comum de uso na lona."
        );

        mockMvc.perform(post("/api/v1/mechanic/inspections/" + ticket.getId() + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.status").value("HUMAN_RESOLVED"))
                .andExpect(jsonPath("$.finalVerdict").value("DESGASTE_NATURAL"))
                .andExpect(jsonPath("$.totalEstimatedBrl").value(0.00))
                .andExpect(jsonPath("$.customerFeedbackText").isNotEmpty());

        // Verify state in DB
        InspectionJpaEntity updated = inspectionRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(InspectionStatus.HUMAN_RESOLVED, updated.getStatus());
        assertEquals(DamageVerdict.DESGASTE_NATURAL, updated.getFinalVerdict());
        assertEquals(DamageVerdict.MAU_USO, updated.getAiVerdict()); // preserved!
        assertEquals(0, BigDecimal.ZERO.compareTo(updated.getTotalEstimatedBrl()));
        assertEquals(0, BigDecimal.ZERO.compareTo(updated.getLaborFeeBrl()));

        // Audit log created
        assertEquals(1, auditLogRepository.count());
        InspectionAuditLogJpaEntity audit = auditLogRepository.findAll().get(0);
        assertEquals(ticket.getId(), audit.getInspectionId());
        assertEquals("HUMAN_TRIAGE", audit.getPreviousStatus());
        assertEquals("HUMAN_RESOLVED", audit.getNewStatus());
        assertEquals("MECHANIC", audit.getActorRole());

        // Analytics outbox created
        assertEquals(1, outboxRepository.count());
        AnalyticsOutboxJpaEntity outbox = outboxRepository.findAll().get(0);
        assertEquals(ticket.getId(), audit.getInspectionId());
        assertTrue(outbox.getPayloadJson().contains("\"aiMatchedMechanic\":false"));
    }

    @Test
    @DisplayName("UC-10-Presential: POST /api/v1/mechanic/inspections/{id}/resolve with requestPresentialInspection marks presential and sets zero cost")
    @WithMockUser(username = "mecanico@mottu-ops.local", roles = "MECHANIC")
    void shouldResolveInspectionWithPresentialConvocation() throws Exception {
        InspectionJpaEntity ticket = createTriageTicket("t-resolve-presential", QueuePriorityReason.LOW_AI_CONFIDENCE, 2, Instant.now());
        ticket.setAiVerdict(DamageVerdict.MAU_USO);
        ticket.setAiConfidence(BigDecimal.valueOf(0.650));
        inspectionRepository.save(ticket);

        ResolveInspectionRequest request = new ResolveInspectionRequest(
                DamageVerdict.DESGASTE_NATURAL,
                List.of(),
                "Foto com reflexo forte, necessário diagnóstico presencial.",
                true
        );

        mockMvc.perform(post("/api/v1/mechanic/inspections/" + ticket.getId() + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.status").value("HUMAN_RESOLVED"))
                .andExpect(jsonPath("$.finalVerdict").value("DESGASTE_NATURAL"))
                .andExpect(jsonPath("$.totalEstimatedBrl").value(0.00))
                .andExpect(jsonPath("$.customerFeedbackText").value(containsString("presencial gratuita")));

        InspectionJpaEntity updated = inspectionRepository.findById(ticket.getId()).orElseThrow();
        assertTrue(updated.isPresentialInspectionRequested());
        assertEquals("Foto com reflexo forte, necessário diagnóstico presencial.", updated.getPresentialRequestReason());
    }

    // =========================================================================
    // UC-09: Rider Single Contestation
    // =========================================================================

    @Test
    @DisplayName("UC-09: Rider contests AUTO_RESOLVED ticket with justification >= 10 chars")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldContestAutoResolvedTicketSuccessfully() throws Exception {
        InspectionJpaEntity ticket = new InspectionJpaEntity();
        ticket.setId(UUID.randomUUID());
        ticket.setIdempotencyKey("idemp-contest-ok");
        ticket.setContractId(contractCarlos.getId());
        ticket.setRiderId(riderCarlos.getId());
        ticket.setMotorcycleId(motoCarlos.getId());
        ticket.setPhotoObjectKey("photos/sample.webp");
        ticket.setPhotoSha256("b".repeat(64));
        ticket.setReportedContext(ReportedOriginContext.RECEIVED_LIKE_THIS);
        ticket.setSafetyRisk(SafetyRiskLevel.SAFE_TO_RIDE);
        ticket.setReportedOdometerKm(1035);
        ticket.setStatus(InspectionStatus.AUTO_RESOLVED);
        ticket.setFinalVerdict(DamageVerdict.MAU_USO);
        ticket.setLaborFeeBrl(BigDecimal.valueOf(35.00));
        ticket.setTotalEstimatedBrl(BigDecimal.valueOf(60.00));
        ticket.setContestedOnce(false);
        ticket.setCreatedAt(Instant.now());
        ticket = inspectionRepository.save(ticket);

        ContestInspectionRequest request = new ContestInspectionRequest(
                "Esse dano já existia quando retirei a moto no galpão."
        );

        mockMvc.perform(post("/api/v1/rider/inspections/" + ticket.getId() + "/contest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.status").value("HUMAN_TRIAGE"))
                .andExpect(jsonPath("$.priorityReason").value("RIDER_CONTESTATION"));

        InspectionJpaEntity contested = inspectionRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(InspectionStatus.HUMAN_TRIAGE, contested.getStatus());
        assertEquals(QueuePriorityReason.RIDER_CONTESTATION, contested.getPriorityReason());
        assertEquals(2, contested.getPriorityWeight());
        assertTrue(contested.isContestedOnce());
        assertEquals("Esse dano já existia quando retirei a moto no galpão.", contested.getContestationReason());
    }

    @Test
    @DisplayName("UC-09: Contest with short justification (<10 chars) returns 400 Bad Request")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldRejectShortContestationReason() throws Exception {
        InspectionJpaEntity ticket = createTicketWithStatus("idemp-contest-short", InspectionStatus.AUTO_RESOLVED);

        ContestInspectionRequest request = new ContestInspectionRequest("não fui");

        mockMvc.perform(post("/api/v1/rider/inspections/" + ticket.getId() + "/contest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UC-09: Contesting an already contested or non-AUTO_RESOLVED ticket returns 422")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldRejectContestWhenAlreadyContestedOrWrongStatus() throws Exception {
        InspectionJpaEntity ticket = createTicketWithStatus("idemp-already-contested", InspectionStatus.AUTO_RESOLVED);
        ticket.setContestedOnce(true);
        inspectionRepository.save(ticket);

        ContestInspectionRequest request = new ContestInspectionRequest("Nova contestação que deve ser rejeitada");

        mockMvc.perform(post("/api/v1/rider/inspections/" + ticket.getId() + "/contest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // =========================================================================
    // UC-07 & UC-08: AI Return Consumer & SLA Recovery
    // =========================================================================

    @Test
    @DisplayName("UC-07: AI result with isValidMotorcyclePhoto = false transitions to INVALID_IMAGE")
    void shouldProcessInvalidImageAiResult() {
        InspectionJpaEntity ticket = createTicketWithStatus("idemp-ai-invalid", InspectionStatus.QUEUED_AI);

        TriageResultPayloadDto payload = new TriageResultPayloadDto(
                ticket.getId(),
                UUID.randomUUID(),
                "SUCCESS",
                false,
                BigDecimal.valueOf(0.1),
                null,
                List.of(),
                "Foto de gato ou objeto que não é moto.",
                "Foto inválida. Por favor, envie uma foto clara da sua moto."
        );

        aiResultConsumer.processAiResult(payload);

        InspectionJpaEntity updated = inspectionRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(InspectionStatus.INVALID_IMAGE, updated.getStatus());
    }

    @Test
    @DisplayName("UC-07: AI result with confidence >= 0.80 transitions to AUTO_RESOLVED")
    void shouldProcessAutoResolvedAiResult() {
        InspectionJpaEntity ticket = createTicketWithStatus("idemp-ai-auto", InspectionStatus.QUEUED_AI);

        TriageResultPayloadDto payload = new TriageResultPayloadDto(
                ticket.getId(),
                UUID.randomUUID(),
                "SUCCESS",
                true,
                BigDecimal.valueOf(0.920),
                DamageVerdict.PRE_EXISTENTE,
                List.of(new MatchedPartDto(partManete.getId(), partManete.getPartName(), partManete.getUnitPriceBrl(), false)),
                "Ralado leve na ponta do manete.",
                "Identificamos defeito pré-existente coberto."
        );

        aiResultConsumer.processAiResult(payload);

        InspectionJpaEntity updated = inspectionRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(InspectionStatus.AUTO_RESOLVED, updated.getStatus());
        assertEquals(DamageVerdict.PRE_EXISTENTE, updated.getFinalVerdict());
        assertEquals(1, inspectionItemRepository.count());
    }

    @Test
    @DisplayName("UC-08: AI unavailable puts ticket in AWAITING_AI_RECOVERY with retry backoff")
    void shouldProcessAiUnavailableResult() {
        InspectionJpaEntity ticket = createTicketWithStatus("idemp-ai-unavail", InspectionStatus.QUEUED_AI);

        TriageResultPayloadDto payload = new TriageResultPayloadDto(
                ticket.getId(),
                UUID.randomUUID(),
                "AI_UNAVAILABLE",
                null, null, null, null, null, null
        );

        aiResultConsumer.processAiResult(payload);

        InspectionJpaEntity updated = inspectionRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(InspectionStatus.AWAITING_AI_RECOVERY, updated.getStatus());
        assertEquals(1, updated.getRetryCount());
        assertTrue(updated.getNextRetryAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("UC-08: SLA Recovery escalates CRITICAL_STOPPED ticket in recovery > 5 min to HUMAN_TRIAGE (weight 1)")
    void shouldEscalateCriticalStoppedTicketAfter5Minutes() {
        InspectionJpaEntity ticket = createTicketWithStatus("idemp-sla-crit", InspectionStatus.AWAITING_AI_RECOVERY);
        ticket.setSafetyRisk(SafetyRiskLevel.CRITICAL_STOPPED);
        ticket.setCreatedAt(Instant.now().minusSeconds(360)); // 6 minutes ago
        ticket.setRetryCount(1);
        inspectionRepository.save(ticket);

        slaRecoveryScheduler.evaluateRecoverySla();

        InspectionJpaEntity escalated = inspectionRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(InspectionStatus.HUMAN_TRIAGE, escalated.getStatus());
        assertEquals(QueuePriorityReason.CRITICAL_SAFETY, escalated.getPriorityReason());
        assertEquals(1, escalated.getPriorityWeight());
    }

    private InspectionJpaEntity createTriageTicket(String idemp, QueuePriorityReason reason, int weight, Instant createdAt) {
        InspectionJpaEntity entity = new InspectionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdempotencyKey(idemp);
        entity.setContractId(contractCarlos.getId());
        entity.setRiderId(riderCarlos.getId());
        entity.setMotorcycleId(motoCarlos.getId());
        entity.setPhotoObjectKey("photos/sample.webp");
        entity.setPhotoSha256("c".repeat(64));
        entity.setReportedContext(ReportedOriginContext.RECEIVED_LIKE_THIS);
        entity.setSafetyRisk(SafetyRiskLevel.SAFE_TO_RIDE);
        entity.setReportedOdometerKm(1035);
        entity.setStatus(InspectionStatus.HUMAN_TRIAGE);
        entity.setPriorityReason(reason);
        entity.setPriorityWeight(weight);
        entity.setLaborFeeBrl(BigDecimal.ZERO);
        entity.setTotalEstimatedBrl(BigDecimal.ZERO);
        entity.setCreatedAt(createdAt);
        return inspectionRepository.save(entity);
    }

    private InspectionJpaEntity createTicketWithStatus(String idemp, InspectionStatus status) {
        InspectionJpaEntity entity = new InspectionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdempotencyKey(idemp);
        entity.setContractId(contractCarlos.getId());
        entity.setRiderId(riderCarlos.getId());
        entity.setMotorcycleId(motoCarlos.getId());
        entity.setPhotoObjectKey("photos/sample.webp");
        entity.setPhotoSha256("d".repeat(64));
        entity.setReportedContext(ReportedOriginContext.RECEIVED_LIKE_THIS);
        entity.setSafetyRisk(SafetyRiskLevel.SAFE_TO_RIDE);
        entity.setReportedOdometerKm(1035);
        entity.setStatus(status);
        entity.setLaborFeeBrl(BigDecimal.ZERO);
        entity.setTotalEstimatedBrl(BigDecimal.ZERO);
        entity.setCreatedAt(Instant.now());
        return inspectionRepository.save(entity);
    }
}
