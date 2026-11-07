package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.*;
import com.mottu.ops.application.port.in.AccidentResultConsumerUseCase;
import com.mottu.ops.application.port.out.AccidentReportRepositoryPort;
import com.mottu.ops.domain.enums.*;
import com.mottu.ops.domain.model.AccidentReport;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.*;
import com.mottu.ops.infrastructure.adapter.out.redis.RedisAccidentQueueAdapter;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AccidentReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataAccidentReportRepository accidentReportRepository;

    @Autowired
    private SpringDataAccidentReportItemRepository accidentItemRepository;

    @Autowired
    private SpringDataAccidentReportPhotoRepository accidentPhotoRepository;

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
    private AccidentResultConsumerUseCase accidentResultConsumer;

    @Autowired
    private RedisAccidentQueueAdapter accidentQueueAdapter;

    private UserJpaEntity riderUser;
    private UserJpaEntity mechanicUser;
    private MotorcycleJpaEntity moto;
    private RentalContractJpaEntity contract;
    private CatalogPartJpaEntity partManete;
    private CatalogPartJpaEntity partRetrovisor;
    private CatalogPartJpaEntity partPedal;
    private CatalogPartJpaEntity partSuporte;

    @BeforeEach
    void setUp() {
        accidentItemRepository.deleteAll();
        accidentPhotoRepository.deleteAll();
        accidentReportRepository.deleteAll();
        outboxRepository.deleteAll();
        rentalContractRepository.deleteAll();
        motorcycleRepository.deleteAll();
        catalogPartRepository.deleteAll();
        userRepository.deleteAll();
        yardRepository.deleteAll();
        accidentQueueAdapter.clearInMemoryJobs();

        YardJpaEntity yard = new YardJpaEntity();
        yard.setId(UUID.randomUUID());
        yard.setName("Galpão Central Mottu");
        yard.setCity("São Paulo");
        yard.setAddress("Rua Mottu, 100");
        yard.setActive(true);
        yardRepository.save(yard);

        riderUser = new UserJpaEntity();
        riderUser.setId(UUID.randomUUID());
        riderUser.setEmail("rider.acidente@mottu.com");
        riderUser.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        riderUser.setFullName("Rider Acidente");
        riderUser.setRole(UserRole.RIDER);
        riderUser.setStatus(AccountStatus.ACTIVE);
        riderUser.setCpf("12345678901");
        riderUser.setPhone("11988887777");
        riderUser.setCreatedAt(Instant.now());
        userRepository.save(riderUser);

        mechanicUser = new UserJpaEntity();
        mechanicUser.setId(UUID.randomUUID());
        mechanicUser.setEmail("mecanico.oficina@mottu.com");
        mechanicUser.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        mechanicUser.setFullName("Mestre Mecânico");
        mechanicUser.setRole(UserRole.MECHANIC);
        mechanicUser.setStatus(AccountStatus.ACTIVE);
        mechanicUser.setCpf("98765432100");
        mechanicUser.setPhone("11977776666");
        mechanicUser.setCreatedAt(Instant.now());
        userRepository.save(mechanicUser);

        moto = new MotorcycleJpaEntity();
        moto.setId(UUID.randomUUID());
        moto.setPlate("BAT1A23");
        moto.setModel(MotorcycleModel.MOTTU_SPORT_110I);
        moto.setStatus(MotorcycleStatus.AVAILABLE);
        moto.setCurrentOdometerKm(1200);
        moto.setYardId(yard.getId());
        moto.setCreatedAt(Instant.now());
        motorcycleRepository.save(moto);

        contract = new RentalContractJpaEntity();
        contract.setId(UUID.randomUUID());
        contract.setRiderId(riderUser.getId());
        contract.setMotorcycleId(moto.getId());
        contract.setActive(true);
        contract.setInitialOdometerKm(1000);
        contract.setPickedUpAt(Instant.now().minusSeconds(86400));
        rentalContractRepository.save(contract);

        partManete = new CatalogPartJpaEntity();
        partManete.setId(UUID.randomUUID());
        partManete.setVtexSku("MAN-001");
        partManete.setPartName("Manete de Freio Direito");
        partManete.setMotorcycleModel(MotorcycleModel.MOTTU_SPORT_110I);
        partManete.setCategory("COMMANDS");
        partManete.setUnitPriceBrl(new BigDecimal("25.00"));
        partManete.setNaturalWear(false);
        partManete.setPriceOverridden(false);
        partManete.setUpdatedAt(Instant.now());
        catalogPartRepository.save(partManete);

        partRetrovisor = new CatalogPartJpaEntity();
        partRetrovisor.setId(UUID.randomUUID());
        partRetrovisor.setVtexSku("RET-001");
        partRetrovisor.setPartName("Retrovisor Direito");
        partRetrovisor.setMotorcycleModel(MotorcycleModel.MOTTU_SPORT_110I);
        partRetrovisor.setCategory("MIRRORS");
        partRetrovisor.setUnitPriceBrl(new BigDecimal("42.50"));
        partRetrovisor.setNaturalWear(false);
        partRetrovisor.setPriceOverridden(false);
        partRetrovisor.setUpdatedAt(Instant.now());
        catalogPartRepository.save(partRetrovisor);

        partPedal = new CatalogPartJpaEntity();
        partPedal.setId(UUID.randomUUID());
        partPedal.setVtexSku("PED-001");
        partPedal.setPartName("Pedal de Apoio Dianteiro Direito");
        partPedal.setMotorcycleModel(MotorcycleModel.MOTTU_SPORT_110I);
        partPedal.setCategory("FRAME");
        partPedal.setUnitPriceBrl(new BigDecimal("80.00"));
        partPedal.setNaturalWear(false);
        partPedal.setPriceOverridden(false);
        partPedal.setUpdatedAt(Instant.now());
        catalogPartRepository.save(partPedal);

        partSuporte = new CatalogPartJpaEntity();
        partSuporte.setId(UUID.randomUUID());
        partSuporte.setVtexSku("SUP-001");
        partSuporte.setPartName("Suporte de Placa");
        partSuporte.setMotorcycleModel(MotorcycleModel.MOTTU_SPORT_110I);
        partSuporte.setCategory("FRAME");
        partSuporte.setUnitPriceBrl(new BigDecimal("35.00"));
        partSuporte.setNaturalWear(false);
        partSuporte.setPriceOverridden(false);
        partSuporte.setUpdatedAt(Instant.now());
        catalogPartRepository.save(partSuporte);
    }

    @Test
    @DisplayName("Gera URL presigned de vídeo respeitando limites de até 30s e 30MB")
    @WithMockUser(username = "rider.acidente@mottu.com", roles = "RIDER")
    void shouldGeneratePresignedVideoUploadUrlSuccessfully() throws Exception {
        AccidentVideoUploadUrlRequest request = new AccidentVideoUploadUrlRequest("video/mp4", 15400000L, 24);

        mockMvc.perform(post("/api/v1/rider/accidents/upload-video-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.videoObjectKey", containsString("accidents/")))
                .andExpect(jsonPath("$.videoObjectKey", endsWith(".mp4")))
                .andExpect(jsonPath("$.expiresInSeconds", is(900)));
    }

    @Test
    @DisplayName("Rejeita vídeo que excede 30 segundos ou 30 MB")
    @WithMockUser(username = "rider.acidente@mottu.com", roles = "RIDER")
    void shouldRejectInvalidVideoUploadUrlParameters() throws Exception {
        AccidentVideoUploadUrlRequest durationTooLong = new AccidentVideoUploadUrlRequest("video/mp4", 1000000L, 35);

        mockMvc.perform(post("/api/v1/rider/accidents/upload-video-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(durationTooLong)))
                .andExpect(status().isBadRequest());

        AccidentVideoUploadUrlRequest sizeTooBig = new AccidentVideoUploadUrlRequest("video/mp4", 35000000L, 20);

        mockMvc.perform(post("/api/v1/rider/accidents/upload-video-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sizeTooBig)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Gera URL presigned de foto para close-up adicional")
    @WithMockUser(username = "rider.acidente@mottu.com", roles = "RIDER")
    void shouldGeneratePresignedPhotoUploadUrl() throws Exception {
        AccidentPhotoUploadUrlRequest request = new AccidentPhotoUploadUrlRequest("image/webp", 450000L);

        mockMvc.perform(post("/api/v1/rider/accidents/upload-photo-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
                .andExpect(jsonPath("$.photoObjectKey", containsString("accidents/")))
                .andExpect(jsonPath("$.photoObjectKey", containsString("photo_")))
                .andExpect(jsonPath("$.photoObjectKey", endsWith(".webp")))
                .andExpect(jsonPath("$.expiresInSeconds", is(300)));
    }

    @Test
    @DisplayName("Cria relato de acidente, bloqueia moto como IN_MAINTENANCE se NEEDS_TOWING, e é idempotente")
    @WithMockUser(username = "rider.acidente@mottu.com", roles = "RIDER")
    void shouldCreateAccidentReportAndLockMotorcycle() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        CreateAccidentReportCommand command = new CreateAccidentReportCommand(
                "accidents/2026/11/video_test.mp4",
                24,
                15400000L,
                List.of("accidents/2026/11/photo_1.webp"),
                false,
                false,
                null,
                VehicleDrivability.NEEDS_TOWING,
                1250,
                "Queda lateral na rotatória molhada",
                "Carenagem ralou levemente"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/rider/accidents")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accidentReportId").isNotEmpty())
                .andExpect(jsonPath("$.status", is("QUEUED_AI")))
                .andExpect(jsonPath("$.motorcycleStatus", is("IN_MAINTENANCE")))
                .andReturn();

        CreateAccidentReportResponse resp = objectMapper.readValue(result.getResponse().getContentAsString(), CreateAccidentReportResponse.class);

        // Verifica persistência no banco
        AccidentReportJpaEntity entity = accidentReportRepository.findById(resp.accidentReportId()).orElseThrow();
        assertEquals(AccidentReportStatus.QUEUED_AI, entity.getStatus());
        assertEquals(VehicleDrivability.NEEDS_TOWING, entity.getDrivability());

        // Verifica bloqueio da motocicleta
        MotorcycleJpaEntity updatedMoto = motorcycleRepository.findById(moto.getId()).orElseThrow();
        assertEquals(MotorcycleStatus.IN_MAINTENANCE, updatedMoto.getStatus());

        // Verifica publicação na fila em memória
        assertFalse(accidentQueueAdapter.getInMemoryJobs().isEmpty());
        assertEquals(resp.accidentReportId(), accidentQueueAdapter.getInMemoryJobs().get(0).accidentReportId());

        // Teste de Idempotência: requisição repetida com o mesmo header
        mockMvc.perform(post("/api/v1/rider/accidents")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accidentReportId", is(resp.accidentReportId().toString())))
                .andExpect(jsonPath("$.status", is("QUEUED_AI")));
    }

    @Test
    @DisplayName("Fluxo Completo: Criação -> Processamento IA (3 peças + guincho) -> Visualização Entregador -> Confirmação Mecânico")
    @WithMockUser(username = "rider.acidente@mottu.com", roles = "RIDER")
    void shouldExecuteFullAccidentLifecycleSuccessfully() throws Exception {
        // 1. Entregador envia relato
        String idempotencyKey = UUID.randomUUID().toString();
        CreateAccidentReportCommand command = new CreateAccidentReportCommand(
                "accidents/2026/11/video_lifecycle.mp4",
                20,
                14000000L,
                List.of("accidents/2026/11/photo_life.webp"),
                false,
                false,
                null,
                VehicleDrivability.NEEDS_TOWING,
                1300,
                "Colisão leve",
                "Verificar freio"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/rider/accidents")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isAccepted())
                .andReturn();

        CreateAccidentReportResponse createResp = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                CreateAccidentReportResponse.class
        );
        UUID reportId = createResp.accidentReportId();

        // 2. Serviço de IA envia resultado do vídeo
        // 3 peças: Manete (25.00), Retrovisor (42.50), Pedal (80.00) => Subtotal: 147.50
        // Mão de obra (3+ peças) => 90.00
        // Guincho (NEEDS_TOWING) => 80.00
        // Total esperado: 147.50 + 90.00 + 80.00 = 317.50
        List<AccidentDeduplicatedPartDto> detectedParts = List.of(
                new AccidentDeduplicatedPartDto(partManete.getId(), partManete.getPartName(), partManete.getUnitPriceBrl(), 1, 6),
                new AccidentDeduplicatedPartDto(partRetrovisor.getId(), partRetrovisor.getPartName(), partRetrovisor.getUnitPriceBrl(), 1, 14),
                new AccidentDeduplicatedPartDto(partPedal.getId(), partPedal.getPartName(), partPedal.getUnitPriceBrl(), 1, 21)
        );

        AccidentResultPayloadDto aiPayload = new AccidentResultPayloadDto(
                reportId,
                UUID.randomUUID(),
                "SUCCESS",
                0.94,
                false,
                "Danos localizados na lateral direita observados no vídeo.",
                detectedParts
        );

        accidentResultConsumer.processResult(aiPayload);

        // 3. Entregador consulta tela T12 (Orçamento)
        mockMvc.perform(get("/api/v1/rider/accidents/" + reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reportId.toString())))
                .andExpect(jsonPath("$.status", is("ESTIMATED_PENDING_WORKSHOP")))
                .andExpect(jsonPath("$.drivability", is("NEEDS_TOWING")))
                .andExpect(jsonPath("$.partsSubtotalBrl", is(147.50)))
                .andExpect(jsonPath("$.laborFeeBrl", is(90.00)))
                .andExpect(jsonPath("$.towingFeeBrl", is(80.00)))
                .andExpect(jsonPath("$.totalEstimatedBrl", is(317.50)))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].videoTimestampSeconds", is(6)))
                .andExpect(jsonPath("$.videoDownloadUrl").isNotEmpty());

        // 4. Mecânico consulta fila de sinistros
        mockMvc.perform(get("/api/v1/mechanic/accidents")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("mecanico.oficina@mottu.com").roles("MECHANIC")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.items[0].id", is(reportId.toString())))
                .andExpect(jsonPath("$.items[0].plate", is("BAT1A23")));

        // 5. Mecânico consulta Dossiê T13
        mockMvc.perform(get("/api/v1/mechanic/accidents/" + reportId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("mecanico.oficina@mottu.com").roles("MECHANIC")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reportId.toString())))
                .andExpect(jsonPath("$.plate", is("BAT1A23")))
                .andExpect(jsonPath("$.videoDownloadUrl").isNotEmpty())
                .andExpect(jsonPath("$.items", hasSize(3)));

        // 6. Mecânico confirma orçamento adicionando Suporte de Placa (35.00)
        // Subtotal passa a ser 147.50 + 35.00 = 182.50
        // Mão de obra (4 peças) => 90.00
        // Guincho => 80.00
        // Novo total: 182.50 + 90.00 + 80.00 = 352.50
        ConfirmAccidentReportCommand confirmCommand = new ConfirmAccidentReportCommand(
                List.of(partManete.getId(), partRetrovisor.getId(), partPedal.getId()),
                List.of(partSuporte.getId()),
                "Confirmadas avarias de queda e adicionado suporte de placa quebrado."
        );

        mockMvc.perform(post("/api/v1/mechanic/accidents/" + reportId + "/confirm")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("mecanico.oficina@mottu.com").roles("MECHANIC"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmCommand)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accidentReportId", is(reportId.toString())))
                .andExpect(jsonPath("$.status", is("WORKSHOP_CONFIRMED")))
                .andExpect(jsonPath("$.totalEstimatedBrl", is(352.50)))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty());

        // 7. Confere persistência final e Outbox
        AccidentReportJpaEntity confirmedEntity = accidentReportRepository.findById(reportId).orElseThrow();
        assertEquals(AccidentReportStatus.WORKSHOP_CONFIRMED, confirmedEntity.getStatus());
        assertEquals(mechanicUser.getId(), confirmedEntity.getReviewedByMechanicId());

        List<AnalyticsOutboxJpaEntity> outboxList = outboxRepository.findAll();
        assertFalse(outboxList.isEmpty());
        assertTrue(outboxList.stream().anyMatch(o -> reportId.equals(o.getAccidentReportId())));
    }
}
