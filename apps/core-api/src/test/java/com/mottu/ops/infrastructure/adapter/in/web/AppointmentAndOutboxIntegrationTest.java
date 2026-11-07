package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.port.in.AnalyticsOutboxExportUseCase;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AppointmentAndOutboxIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataInspectionRepository inspectionRepository;

    @Autowired
    private SpringDataYardAppointmentRepository appointmentRepository;

    @Autowired
    private SpringDataAnalyticsOutboxRepository outboxRepository;

    @Autowired
    private SpringDataCatalogPartRepository catalogPartRepository;

    @Autowired
    private SpringDataSystemSettingsRepository settingsRepository;

    @Autowired
    private SpringDataRentalContractRepository rentalContractRepository;

    @Autowired
    private SpringDataMotorcycleRepository motorcycleRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataYardRepository yardRepository;

    @Autowired
    private SpringDataInspectionAuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AnalyticsOutboxExportUseCase outboxExportUseCase;

    private UserJpaEntity riderCarlos;
    private UserJpaEntity managerUser;
    private YardJpaEntity yardButanta;
    private MotorcycleJpaEntity motoCarlos;
    private RentalContractJpaEntity contractCarlos;
    private InspectionJpaEntity inspectionResolved;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        outboxRepository.deleteAll();
        auditLogRepository.deleteAll();
        inspectionRepository.deleteAll();
        rentalContractRepository.deleteAll();
        motorcycleRepository.deleteAll();
        catalogPartRepository.deleteAll();
        userRepository.deleteAll();
        yardRepository.deleteAll();

        // 1. Galpão Butantã
        yardButanta = yardRepository.save(new YardJpaEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Galpão Mottu Butantã",
                "São Paulo",
                "Av. Corifeu, 1000",
                true
        ));

        // 2. Usuário Entregador Carlos
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

        // 3. Usuário Gestor
        managerUser = new UserJpaEntity();
        managerUser.setId(UUID.fromString("e0000000-0000-0000-0000-000000000001"));
        managerUser.setFullName("Gestor Master");
        managerUser.setCpf("11122233344");
        managerUser.setPhone("11999990001");
        managerUser.setEmail("gestor@mottu-ops.local");
        managerUser.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        managerUser.setRole(UserRole.MANAGER);
        managerUser.setStatus(AccountStatus.ACTIVE);
        managerUser.setCreatedAt(Instant.now());
        managerUser = userRepository.save(managerUser);

        // 4. Motocicleta alugada por Carlos
        motoCarlos = motorcycleRepository.save(new MotorcycleJpaEntity(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "MOT1A23",
                MotorcycleModel.MOTTU_SPORT_110I,
                yardButanta.getId(),
                MotorcycleStatus.RENTED,
                1000,
                Instant.now().minusSeconds(3600 * 12)
        ));

        // 5. Contrato ativo
        contractCarlos = rentalContractRepository.save(new RentalContractJpaEntity(
                UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                riderCarlos.getId(),
                motoCarlos.getId(),
                Instant.now().minusSeconds(3600 * 12),
                1000,
                null,
                true
        ));

        // 6. Vistoria AUTO_RESOLVED pertencente a Carlos
        inspectionResolved = new InspectionJpaEntity();
        inspectionResolved.setId(UUID.randomUUID());
        inspectionResolved.setIdempotencyKey("idemp-task12-test-01");
        inspectionResolved.setContractId(contractCarlos.getId());
        inspectionResolved.setRiderId(riderCarlos.getId());
        inspectionResolved.setMotorcycleId(motoCarlos.getId());
        inspectionResolved.setPhotoObjectKey("inspections/2026/11/manete-test.webp");
        inspectionResolved.setPhotoSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        inspectionResolved.setFraudSuspected(false);
        inspectionResolved.setReportedContext(ReportedOriginContext.ACCIDENT_OR_DROP);
        inspectionResolved.setSafetyRisk(SafetyRiskLevel.SAFE_TO_RIDE);
        inspectionResolved.setReportedOdometerKm(1035);
        inspectionResolved.setStatus(InspectionStatus.AUTO_RESOLVED);
        inspectionResolved.setFinalVerdict(DamageVerdict.MAU_USO);
        inspectionResolved.setAiConfidence(new BigDecimal("0.920"));
        inspectionResolved.setLaborFeeBrl(new BigDecimal("35.00"));
        inspectionResolved.setTotalEstimatedBrl(new BigDecimal("60.00"));
        inspectionResolved.setCustomerFeedbackText("Identificamos avaria por queda no manete.");
        inspectionResolved.setCreatedAt(Instant.now().minusSeconds(300));
        inspectionResolved.setResolvedAt(Instant.now().minusSeconds(200));
        inspectionResolved = inspectionRepository.save(inspectionResolved);
    }

    // ==========================================
    // UC-12: Agendamento de Galpão
    // ==========================================

    @Test
    @DisplayName("UC-12.1: Deve rejeitar agendamento com data no passado com HTTP 400")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = {"RIDER"})
    void shouldRejectAppointmentWithPastDate() throws Exception {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                yardButanta.getId(),
                LocalDate.now().minusDays(1),
                AppointmentShift.MORNING
        );

        mockMvc.perform(post("/api/v1/rider/inspections/{id}/appointment", inspectionResolved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("A data do agendamento não pode ser no passado")));
    }

    @Test
    @DisplayName("UC-12.2: Deve criar agendamento com sucesso, transicionar para SCHEDULED e gravar em yard_appointments")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = {"RIDER"})
    void shouldCreateAppointmentSuccessfullyAndSetStatusScheduled() throws Exception {
        LocalDate appointmentDate = LocalDate.now().plusDays(2);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                yardButanta.getId(),
                appointmentDate,
                AppointmentShift.MORNING
        );

        mockMvc.perform(post("/api/v1/rider/inspections/{id}/appointment", inspectionResolved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentId").isNotEmpty())
                .andExpect(jsonPath("$.inspectionStatus").value("SCHEDULED"))
                .andExpect(jsonPath("$.yardName").value("Galpão Mottu Butantã"))
                .andExpect(jsonPath("$.scheduledDate").value(appointmentDate.toString()))
                .andExpect(jsonPath("$.shift").value("MORNING"));

        // Verificar tabela yard_appointments
        assertTrue(appointmentRepository.existsByInspectionId(inspectionResolved.getId()));
        YardAppointmentJpaEntity savedAppointment = appointmentRepository.findByInspectionId(inspectionResolved.getId()).orElseThrow();
        assertEquals(yardButanta.getId(), savedAppointment.getYardId());
        assertEquals(appointmentDate, savedAppointment.getScheduledDate());
        assertEquals(AppointmentShift.MORNING, savedAppointment.getShift());

        // Verificar atualização de status na vistoria
        InspectionJpaEntity updatedInspection = inspectionRepository.findById(inspectionResolved.getId()).orElseThrow();
        assertEquals(InspectionStatus.SCHEDULED, updatedInspection.getStatus());

        // Verificar GET /api/v1/rider/inspections/{id} inclui os dados do agendamento
        mockMvc.perform(get("/api/v1/rider/inspections/{id}", inspectionResolved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.appointment.yardName").value("Galpão Mottu Butantã"))
                .andExpect(jsonPath("$.appointment.shift").value("MORNING"))
                .andExpect(jsonPath("$.appointment.scheduledDate").value(appointmentDate.toString()));
    }

    @Test
    @DisplayName("UC-12.3: Rejeita agendamento quando galpão não existe ou está inativo")
    @WithMockUser(username = "entregador@mottu-ops.local", roles = {"RIDER"})
    void shouldRejectAppointmentWhenYardNotFound() throws Exception {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                UUID.randomUUID(),
                LocalDate.now().plusDays(1),
                AppointmentShift.AFTERNOON
        );

        mockMvc.perform(post("/api/v1/rider/inspections/{id}/appointment", inspectionResolved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // UC-12: BigQuery Outbox Export em Lote
    // ==========================================

    @Test
    @DisplayName("UC-12.4: Deve ler registros pendentes em analytics_outbox e marcar exported=true no modo offline")
    void shouldExportPendingOutboxBatchInOfflineMode() {
        // Criar 5 registros não exportados
        for (int i = 0; i < 5; i++) {
            outboxRepository.save(new AnalyticsOutboxJpaEntity(
                    UUID.randomUUID(),
                    inspectionResolved.getId(),
                    "{\"testIndex\":" + i + "}",
                    false,
                    Instant.now().minusSeconds(60 - i),
                    null
            ));
        }

        assertEquals(5, outboxRepository.findByExportedFalseOrderByCreatedAtAsc().size());

        // Executar exportação de lote
        int exportedCount = outboxExportUseCase.exportPendingBatch();

        assertEquals(5, exportedCount);
        assertEquals(0, outboxRepository.findByExportedFalseOrderByCreatedAtAsc().size());

        List<AnalyticsOutboxJpaEntity> allRows = outboxRepository.findAll();
        assertEquals(5, allRows.size());
        for (AnalyticsOutboxJpaEntity row : allRows) {
            assertTrue(row.isExported());
            assertNotNull(row.getExportedAt());
        }
    }

    // ==========================================
    // Gestor: Catálogo Mottu Store
    // ==========================================

    @Test
    @DisplayName("Gestor: POST /api/v1/manager/catalog/sync-mottu-store sincroniza catálogo com sucesso")
    @WithMockUser(username = "gestor@mottu-ops.local", roles = {"MANAGER"})
    void shouldSyncCatalogWithMottuStore() throws Exception {
        mockMvc.perform(post("/api/v1/manager/catalog/sync-mottu-store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount").isNumber())
                .andExpect(jsonPath("$.usedFallback").isBoolean())
                .andExpect(jsonPath("$.syncedAt").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Catálogo sincronizado com loja.mottu.com.br!"));
    }

    @Test
    @DisplayName("Gestor: GET /api/v1/manager/catalog/parts lista e filtra peças do catálogo")
    @WithMockUser(username = "gestor@mottu-ops.local", roles = {"MANAGER"})
    void shouldListAndFilterCatalogParts() throws Exception {
        catalogPartRepository.save(new CatalogPartJpaEntity(
                UUID.randomUUID(),
                "VTEX-MANETE-01",
                MotorcycleModel.MOTTU_SPORT_110I,
                "Manete de Freio Direito",
                "Comandos",
                new BigDecimal("25.00"),
                false,
                false,
                "https://images.mottu.com/manete.webp",
                Instant.now()
        ));

        catalogPartRepository.save(new CatalogPartJpaEntity(
                UUID.randomUUID(),
                "VTEX-LONA-02",
                MotorcycleModel.TVS_APACHE_160,
                "Lona de Freio Traseira",
                "Freios",
                new BigDecimal("45.00"),
                true,
                false,
                "https://images.mottu.com/lona.webp",
                Instant.now()
        ));

        mockMvc.perform(get("/api/v1/manager/catalog/parts")
                        .param("model", "MOTTU_SPORT_110I")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].vtexSku").value("VTEX-MANETE-01"))
                .andExpect(jsonPath("$.items[0].motorcycleModel").value("MOTTU_SPORT_110I"))
                .andExpect(jsonPath("$.items[0].partName").value("Manete de Freio Direito"))
                .andExpect(jsonPath("$.items[0].unitPriceBrl").value(25.00))
                .andExpect(jsonPath("$.items[0].priceOverridden").value(false));
    }

    @Test
    @DisplayName("Gestor: PATCH /api/v1/manager/catalog/parts/{id} atualiza preço e marca price_overridden=true")
    @WithMockUser(username = "gestor@mottu-ops.local", roles = {"MANAGER"})
    void shouldPatchCatalogPartAndSetPriceOverridden() throws Exception {
        CatalogPartJpaEntity part = catalogPartRepository.save(new CatalogPartJpaEntity(
                UUID.randomUUID(),
                "VTEX-MANETE-01",
                MotorcycleModel.MOTTU_SPORT_110I,
                "Manete de Freio Direito",
                "Comandos",
                new BigDecimal("25.00"),
                false,
                false,
                "https://images.mottu.com/manete.webp",
                Instant.now()
        ));

        UpdateCatalogPartRequest patchRequest = new UpdateCatalogPartRequest(
                new BigDecimal("29.90"),
                false
        );

        mockMvc.perform(patch("/api/v1/manager/catalog/parts/{id}", part.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPriceBrl").value(29.90))
                .andExpect(jsonPath("$.priceOverridden").value(true));

        CatalogPartJpaEntity reloaded = catalogPartRepository.findById(part.getId()).orElseThrow();
        assertEquals(new BigDecimal("29.90"), reloaded.getUnitPriceBrl());
        assertTrue(reloaded.isPriceOverridden());
    }

    // ==========================================
    // Gestor: Configurações Globais
    // ==========================================

    @Test
    @DisplayName("Gestor: GET e PATCH /api/v1/manager/settings consulta e atualiza taxa padrão")
    @WithMockUser(username = "gestor@mottu-ops.local", roles = {"MANAGER"})
    void shouldGetAndPatchManagerSettings() throws Exception {
        settingsRepository.save(new SystemSettingsJpaEntity("STANDARD_LABOR_FEE_BRL", "35.00", Instant.now()));

        // GET
        mockMvc.perform(get("/api/v1/manager/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standardLaborFeeBrl").value(35.00));

        // PATCH
        ManagerSettingsDto patchRequest = new ManagerSettingsDto(new BigDecimal("42.00"));
        mockMvc.perform(patch("/api/v1/manager/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standardLaborFeeBrl").value(42.00));

        // GET verificação
        mockMvc.perform(get("/api/v1/manager/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standardLaborFeeBrl").value(42.00));
    }

    // ==========================================
    // Gestor: Analytics Summary
    // ==========================================

    @Test
    @DisplayName("Gestor: GET /api/v1/manager/analytics/summary retorna métricas agregadas reais")
    @WithMockUser(username = "gestor@mottu-ops.local", roles = {"MANAGER"})
    void shouldGetManagerAnalyticsSummary() throws Exception {
        mockMvc.perform(get("/api/v1/manager/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeMotorcyclesCount").isNumber())
                .andExpect(jsonPath("$.autoResolvedPercentage").isNumber())
                .andExpect(jsonPath("$.currentHumanQueueSize").isNumber())
                .andExpect(jsonPath("$.estimatedMechanicHoursSaved").isNumber())
                .andExpect(jsonPath("$.lookerStudioEmbedUrl").isNotEmpty());
    }
}
