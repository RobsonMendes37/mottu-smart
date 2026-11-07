package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.CreateInspectionRequest;
import com.mottu.ops.application.port.out.StoragePort;
import com.mottu.ops.domain.enums.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.InspectionJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.YardJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataInspectionRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataMotorcycleRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataRentalContractRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataYardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InspectionCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataInspectionRepository inspectionRepository;

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

    @MockBean
    private StoragePort storagePort;

    private UserJpaEntity riderCarlos;
    private UserJpaEntity riderOther;
    private MotorcycleJpaEntity motoCarlos;
    private RentalContractJpaEntity contractCarlos;

    @BeforeEach
    void setUp() {
        inspectionRepository.deleteAll();
        rentalContractRepository.deleteAll();
        motorcycleRepository.deleteAll();
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

        riderOther = new UserJpaEntity();
        riderOther.setId(UUID.fromString("e0000000-0000-0000-0000-000000000099"));
        riderOther.setFullName("Outro Entregador");
        riderOther.setCpf("99988877766");
        riderOther.setPhone("11987654399");
        riderOther.setEmail("outro@mottu-ops.local");
        riderOther.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        riderOther.setRole(UserRole.RIDER);
        riderOther.setStatus(AccountStatus.ACTIVE);
        riderOther.setCreatedAt(Instant.now());
        riderOther = userRepository.save(riderOther);

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

        // Default mock for storage
        when(storagePort.objectExists(anyString())).thenReturn(true);
        when(storagePort.getObjectBytes(anyString())).thenReturn("mock-photo-bytes-clean-inspection".getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================================
    // UC-03: Triple Click 3G Idempotency & Max 3 Open Inspections
    // =========================================================================

    @Test
    @DisplayName("UC-03: Same Idempotency-Key sent 3 times concurrently creates only 1 inspection in DB")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldHandleTripleClickIdempotencyGracefully() throws Exception {
        String idempotencyKey = "idemp-triple-click-12345";
        CreateInspectionRequest request = new CreateInspectionRequest(
                "inspections/2026/11/manete-01.webp",
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1035,
                "Manete direito com ralado ao retirar moto."
        );

        String jsonBody = objectMapper.writeValueAsString(request);

        // 1st request
        MvcResult res1 = mockMvc.perform(post("/api/v1/rider/inspections")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.inspectionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED_AI"))
                .andReturn();

        String inspectionId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("inspectionId").asText();

        // 2nd request (duplicate)
        MvcResult res2 = mockMvc.perform(post("/api/v1/rider/inspections")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.inspectionId").value(inspectionId1))
                .andExpect(jsonPath("$.status").value("QUEUED_AI"))
                .andReturn();

        // 3rd request (duplicate)
        MvcResult res3 = mockMvc.perform(post("/api/v1/rider/inspections")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.inspectionId").value(inspectionId1))
                .andExpect(jsonPath("$.status").value("QUEUED_AI"))
                .andReturn();

        // Verify only 1 record exists in database
        assertEquals(1, inspectionRepository.count());
    }

    @Test
    @DisplayName("UC-03: Rider with 3 open inspections cannot create a 4th (422 Unprocessable Entity)")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldRejectFourthOpenInspectionWhenQuotaReached() throws Exception {
        // Create 3 open inspections (QUEUED_AI, AWAITING_AI_RECOVERY, HUMAN_TRIAGE)
        createOpenInspection("idemp-open-1", InspectionStatus.QUEUED_AI);
        createOpenInspection("idemp-open-2", InspectionStatus.AWAITING_AI_RECOVERY);
        createOpenInspection("idemp-open-3", InspectionStatus.HUMAN_TRIAGE);

        assertEquals(3, inspectionRepository.count());

        CreateInspectionRequest request = new CreateInspectionRequest(
                "inspections/2026/11/manete-4.webp",
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1050,
                "Tentativa de abrir quarto chamado"
        );

        mockMvc.perform(post("/api/v1/rider/inspections")
                        .header("Idempotency-Key", "idemp-open-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de Negócio Violada"))
                .andExpect(jsonPath("$.detail", containsString("Limite de 3 chamados em aberto atingido")));

        assertEquals(3, inspectionRepository.count());
    }

    // =========================================================================
    // UC-04: Photo Anti-Fraud (SHA-256 and Perceptual dHash)
    // =========================================================================

    @Test
    @DisplayName("UC-04: Existing photo SHA-256 collision routes directly to HUMAN_TRIAGE with FRAUD_SUSPICION (weight 4)")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldFlagFraudOnSha256Collision() throws Exception {
        byte[] photoBytes = "fraudulent-collision-photo-data".getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String sha256 = HexFormat.of().formatHex(digest.digest(photoBytes));

        // Create past inspection with this SHA-256
        InspectionJpaEntity past = new InspectionJpaEntity();
        past.setId(UUID.randomUUID());
        past.setIdempotencyKey("idemp-past-sha");
        past.setContractId(contractCarlos.getId());
        past.setRiderId(riderCarlos.getId());
        past.setMotorcycleId(motoCarlos.getId());
        past.setPhotoObjectKey("inspections/2026/11/old.webp");
        past.setPhotoSha256(sha256);
        past.setReportedContext(ReportedOriginContext.RECEIVED_LIKE_THIS);
        past.setSafetyRisk(SafetyRiskLevel.SAFE_TO_RIDE);
        past.setReportedOdometerKm(1000);
        past.setStatus(InspectionStatus.CLOSED);
        past.setLaborFeeBrl(BigDecimal.ZERO);
        past.setTotalEstimatedBrl(BigDecimal.ZERO);
        past.setCreatedAt(Instant.now().minusSeconds(86400));
        inspectionRepository.save(past);

        when(storagePort.getObjectBytes("inspections/2026/11/new-fraud.webp")).thenReturn(photoBytes);

        CreateInspectionRequest request = new CreateInspectionRequest(
                "inspections/2026/11/new-fraud.webp",
                ReportedOriginContext.RECEIVED_LIKE_THIS,
                SafetyRiskLevel.SAFE_TO_RIDE,
                1035,
                "Enviando foto antiga repetida"
        );

        mockMvc.perform(post("/api/v1/rider/inspections")
                        .header("Idempotency-Key", "idemp-fraud-attempt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("HUMAN_TRIAGE"));

        InspectionJpaEntity created = inspectionRepository.findByIdempotencyKey("idemp-fraud-attempt").orElseThrow();
        assertEquals(InspectionStatus.HUMAN_TRIAGE, created.getStatus());
        assertEquals(QueuePriorityReason.FRAUD_SUSPICION, created.getPriorityReason());
        assertEquals(4, created.getPriorityWeight());
        assertEquals(true, created.isFraudSuspected());
    }

    // =========================================================================
    // Inspection Details & IDOR Security
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/rider/inspections/{id} returns 404 for another rider's inspection (IDOR Guard)")
    @WithMockUser(username = "outro@mottu-ops.local", roles = "RIDER")
    void shouldReturn404WhenAccessingOtherRidersInspection() throws Exception {
        InspectionJpaEntity inspection = createOpenInspection("idemp-carlos-secret", InspectionStatus.AUTO_RESOLVED);

        mockMvc.perform(get("/api/v1/rider/inspections/" + inspection.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/rider/inspections/{id} returns full inspection details for owner")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldReturnFullInspectionDetailsForOwner() throws Exception {
        InspectionJpaEntity inspection = createOpenInspection("idemp-carlos-view", InspectionStatus.AUTO_RESOLVED);
        inspection.setFinalVerdict(DamageVerdict.PRE_EXISTENTE);
        inspection.setAiConfidence(BigDecimal.valueOf(0.920));
        inspection.setCustomerFeedbackText("Vistoria aprovada sem custos.");
        inspection.setResolvedAt(Instant.now());
        inspectionRepository.save(inspection);

        mockMvc.perform(get("/api/v1/rider/inspections/" + inspection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inspection.getId().toString()))
                .andExpect(jsonPath("$.status").value("AUTO_RESOLVED"))
                .andExpect(jsonPath("$.finalVerdict").value("PRE_EXISTENTE"))
                .andExpect(jsonPath("$.canContest").value(true))
                .andExpect(jsonPath("$.contestedOnce").value(false))
                .andExpect(jsonPath("$.photoDownloadUrl").isNotEmpty());
    }

    // =========================================================================
    // SSE Stream
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/rider/inspections/{id}/stream connects to Server-Sent Events stream")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldConnectToSseStream() throws Exception {
        InspectionJpaEntity inspection = createOpenInspection("idemp-sse-test", InspectionStatus.QUEUED_AI);

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/rider/inspections/" + inspection.getId() + "/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertNotNull(mvcResult);
    }

    @Test
    @DisplayName("GET /api/v1/inspections/{id}/live-status alias connects to Server-Sent Events stream")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    void shouldConnectToLiveStatusAliasStream() throws Exception {
        InspectionJpaEntity inspection = createOpenInspection("idemp-sse-alias", InspectionStatus.QUEUED_AI);

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/inspections/" + inspection.getId() + "/live-status")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertNotNull(mvcResult);
    }

    private InspectionJpaEntity createOpenInspection(String idempotencyKey, InspectionStatus status) {
        InspectionJpaEntity entity = new InspectionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setContractId(contractCarlos.getId());
        entity.setRiderId(riderCarlos.getId());
        entity.setMotorcycleId(motoCarlos.getId());
        entity.setPhotoObjectKey("inspections/2026/11/sample.webp");
        entity.setPhotoSha256("a".repeat(64));
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
