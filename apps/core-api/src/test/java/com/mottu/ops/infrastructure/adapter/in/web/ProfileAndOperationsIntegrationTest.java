package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.CreateAppointmentRequest;
import com.mottu.ops.domain.enums.*;
import com.mottu.ops.infrastructure.adapter.in.web.dto.mechanic.CreateServiceOrderRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.mechanic.UpdateStockRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UpdatePasswordRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.profile.UpdateProfileRequestDto;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProfileAndOperationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataMotorcycleRepository motorcycleRepository;

    @Autowired
    private SpringDataRentalContractRepository rentalContractRepository;

    @Autowired
    private SpringDataYardRepository yardRepository;

    @Autowired
    private SpringDataCatalogPartRepository catalogPartRepository;

    @Autowired
    private SpringDataServiceOrderRepository serviceOrderRepository;

    @Autowired
    private SpringDataInspectionRepository inspectionRepository;

    @Autowired
    private SpringDataInspectionItemRepository inspectionItemRepository;

    @Autowired
    private SpringDataYardAppointmentRepository appointmentRepository;

    @Autowired
    private SpringDataAnalyticsOutboxRepository outboxRepository;

    @Autowired
    private SpringDataInspectionAuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserJpaEntity riderUser;
    private UserJpaEntity mechanicUser;
    private MotorcycleJpaEntity testMoto;
    private RentalContractJpaEntity testContract;
    private CatalogPartJpaEntity testPart;
    private YardJpaEntity testYard;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        auditLogRepository.deleteAll();
        appointmentRepository.deleteAll();
        serviceOrderRepository.deleteAll();
        inspectionItemRepository.deleteAll();
        inspectionRepository.deleteAll();
        rentalContractRepository.deleteAll();
        motorcycleRepository.deleteAll();
        catalogPartRepository.deleteAll();
        userRepository.deleteAll();
        yardRepository.deleteAll();

        testYard = yardRepository.save(new YardJpaEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Galpão Butantã", "São Paulo", "Av. Corifeu, 1000", true
        ));

        riderUser = new UserJpaEntity();
        riderUser.setId(UUID.fromString("e0000000-0000-0000-0000-000000000003"));
        riderUser.setFullName("Carlos Entregador");
        riderUser.setCpf("33344455566");
        riderUser.setPhone("11987654323");
        riderUser.setEmail("entregador@mottu-ops.local");
        riderUser.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        riderUser.setPasswordSha256("e4948ce65b16f387db2c451ca5d8009623e1f021e1a5fdfdfa15998df90ff5b0");
        riderUser.setRole(UserRole.RIDER);
        riderUser.setStatus(AccountStatus.ACTIVE);
        riderUser.setPreferredUnit("Unidade Butantã (SP-04)");
        riderUser.setCnhStatus("Categoria A • Válida até 08/2029");
        riderUser.setEmployeeCode("CTR-2026-8841");
        riderUser.setRoleTitle("Entregador parceiro");
        riderUser.setShiftLabel("Plano Minha Mottu Semanal");
        riderUser.setCreatedAt(Instant.now());
        riderUser = userRepository.save(riderUser);

        mechanicUser = new UserJpaEntity();
        mechanicUser.setId(UUID.fromString("e0000000-0000-0000-0000-000000000002"));
        mechanicUser.setFullName("Mecânico Chefe");
        mechanicUser.setCpf("22233344455");
        mechanicUser.setPhone("11987654322");
        mechanicUser.setEmail("mecanico@mottu-ops.local");
        mechanicUser.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        mechanicUser.setPasswordSha256("e4948ce65b16f387db2c451ca5d8009623e1f021e1a5fdfdfa15998df90ff5b0");
        mechanicUser.setRole(UserRole.MECHANIC);
        mechanicUser.setStatus(AccountStatus.ACTIVE);
        mechanicUser.setCreatedAt(Instant.now());
        mechanicUser = userRepository.save(mechanicUser);

        testMoto = new MotorcycleJpaEntity(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "MOT1A23",
                MotorcycleModel.MOTTU_SPORT_110I,
                testYard.getId(),
                MotorcycleStatus.RENTED,
                1200,
                Instant.now()
        );
        testMoto = motorcycleRepository.save(testMoto);

        testContract = new RentalContractJpaEntity(
                UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                riderUser.getId(),
                testMoto.getId(),
                Instant.now(),
                1200,
                null,
                true
        );
        testContract.setContractCode("CTR-2026-8841");
        testContract = rentalContractRepository.save(testContract);

        testPart = new CatalogPartJpaEntity(
                UUID.fromString("77777777-7777-7777-7777-777777777771"),
                "SKU-SPORT-001",
                MotorcycleModel.MOTTU_SPORT_110I,
                "Manete de Freio Direito",
                "Comandos & Freios",
                new BigDecimal("25.00"),
                false,
                false,
                "http://minio:9000/parts/manete.jpg",
                Instant.now()
        );
        testPart.setStockQuantity(4);
        testPart.setMinStockQuantity(8);
        testPart.setAisleBin("Corredor A • Gaveta 04");
        testPart = catalogPartRepository.save(testPart);
    }

    @Test
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    @DisplayName("GET /api/v1/profile/me returns complete profile with activePlate and contractCode")
    void shouldReturnProfileSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(riderUser.getId().toString()))
                .andExpect(jsonPath("$.fullName").value("Carlos Entregador"))
                .andExpect(jsonPath("$.email").value("entregador@mottu-ops.local"))
                .andExpect(jsonPath("$.activePlate").value("MOT1A23"))
                .andExpect(jsonPath("$.contractCode").value("CTR-2026-8841"))
                .andExpect(jsonPath("$.role").value("RIDER"))
                .andExpect(jsonPath("$.passwordSha256Fingerprint").value(startsWith("e4948ce6")));
    }

    @Test
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    @DisplayName("PUT /api/v1/profile/me updates user personal information")
    void shouldUpdateProfileSuccessfully() throws Exception {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto(
                "Carlos Silva Novo",
                "(11) 99999-8888",
                "entregador@mottu-ops.local",
                "carlos.pix@banco.com",
                "Unidade Lapa (SP-02)",
                "Entregador Master"
        );

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Carlos Silva Novo"))
                .andExpect(jsonPath("$.phone").value("(11) 99999-8888"))
                .andExpect(jsonPath("$.pixKey").value("carlos.pix@banco.com"))
                .andExpect(jsonPath("$.preferredUnit").value("Unidade Lapa (SP-02)"))
                .andExpect(jsonPath("$.roleTitle").value("Entregador Master"));

        UserJpaEntity updated = userRepository.findById(riderUser.getId()).orElseThrow();
        assertEquals("Carlos Silva Novo", updated.getFullName());
        assertEquals("(11) 99999-8888", updated.getPhone());
    }

    @Test
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    @DisplayName("PUT /api/v1/profile/me/password rejects invalid current password")
    void shouldRejectInvalidCurrentPassword() throws Exception {
        UpdatePasswordRequestDto request = new UpdatePasswordRequestDto(
                "SenhaErrada@123",
                "NovaSenha@2026#",
                null
        );

        mockMvc.perform(put("/api/v1/profile/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    @DisplayName("PUT /api/v1/profile/me/password rejects password reuse with 422")
    void shouldRejectPasswordReuse() throws Exception {
        UpdatePasswordRequestDto request = new UpdatePasswordRequestDto(
                "Mottu@2026!",
                "Mottu@2026!",
                null
        );

        mockMvc.perform(put("/api/v1/profile/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(containsString("diferente da senha atual")));
    }

    @Test
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    @DisplayName("PUT /api/v1/profile/me/password updates password with SHA-256 fingerprint")
    void shouldUpdatePasswordSuccessfully() throws Exception {
        UpdatePasswordRequestDto request = new UpdatePasswordRequestDto(
                "Mottu@2026!",
                "MinhaNovaSenha@2026!",
                null
        );

        mockMvc.perform(put("/api/v1/profile/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("criptografia SHA-256 + BCrypt")))
                .andExpect(jsonPath("$.passwordSha256Fingerprint").isNotEmpty());

        UserJpaEntity updated = userRepository.findById(riderUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("MinhaNovaSenha@2026!", updated.getPasswordHash()));
        assertNotNull(updated.getPasswordSha256());
    }

    @Test
    @WithMockUser(username = "mecanico@mottu-ops.local", roles = "MECHANIC")
    @DisplayName("GET /api/v1/mechanic/operations lists service orders and stock items")
    void shouldListMechanicOperations() throws Exception {
        ServiceOrderJpaEntity order = new ServiceOrderJpaEntity(
                UUID.randomUUID(),
                "OS-2026-0412",
                testMoto.getId(),
                null,
                testYard.getId(),
                "Baia 01 • Urgente",
                "Troca de manete direito",
                "Vistoria #8841",
                25,
                ServiceOrderStatus.IN_REPAIR,
                Instant.now(),
                Instant.now()
        );
        serviceOrderRepository.save(order);

        mockMvc.perform(get("/api/v1/mechanic/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceOrders", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.serviceOrders[0].code").value("OS-2026-0412"))
                .andExpect(jsonPath("$.serviceOrders[0].plate").value("MOT1A23"))
                .andExpect(jsonPath("$.stockItems", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.stockItems[0].sku").value("SKU-SPORT-001"))
                .andExpect(jsonPath("$.stockItems[0].quantity").value(4));
    }

    @Test
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    @DisplayName("GET /api/v1/mechanic/operations is forbidden for RIDER")
    void shouldForbidRiderFromAccessingMechanicOperations() throws Exception {
        mockMvc.perform(get("/api/v1/mechanic/operations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mecanico@mottu-ops.local", roles = "MECHANIC")
    @DisplayName("POST /api/v1/mechanic/operations/orders creates a new service order")
    void shouldCreateServiceOrder() throws Exception {
        CreateServiceOrderRequestDto request = new CreateServiceOrderRequestDto(
                "MOT1A23",
                "Baia 02 • Rápida",
                "Revisão preventiva de freio"
        );

        mockMvc.perform(post("/api/v1/mechanic/operations/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plate").value("MOT1A23"))
                .andExpect(jsonPath("$.bay").value("Baia 02 • Rápida"))
                .andExpect(jsonPath("$.status").value("WAITING_BAY"));
    }

    @Test
    @WithMockUser(username = "mecanico@mottu-ops.local", roles = "MECHANIC")
    @DisplayName("PATCH /api/v1/mechanic/operations/orders/{id}/advance transitions status")
    void shouldAdvanceOrderStatusAndTransitionMotorcycle() throws Exception {
        ServiceOrderJpaEntity order = new ServiceOrderJpaEntity(
                UUID.randomUUID(),
                "OS-2026-9901",
                testMoto.getId(),
                null,
                testYard.getId(),
                "Baia 01",
                "Troca de peça",
                "Vistoria",
                30,
                ServiceOrderStatus.WAITING_BAY,
                Instant.now(),
                Instant.now()
        );
        order = serviceOrderRepository.save(order);

        // Advance 1: WAITING_BAY -> IN_REPAIR -> Moto becomes IN_MAINTENANCE
        mockMvc.perform(patch("/api/v1/mechanic/operations/orders/{id}/advance", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REPAIR"));

        MotorcycleJpaEntity inMaint = motorcycleRepository.findById(testMoto.getId()).orElseThrow();
        assertEquals(MotorcycleStatus.IN_MAINTENANCE, inMaint.getStatus());

        // Advance 2: IN_REPAIR -> READY_PICKUP -> Moto has active contract, so returns to RENTED
        mockMvc.perform(patch("/api/v1/mechanic/operations/orders/{id}/advance", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_PICKUP"));

        MotorcycleJpaEntity rentedAgain = motorcycleRepository.findById(testMoto.getId()).orElseThrow();
        assertEquals(MotorcycleStatus.RENTED, rentedAgain.getStatus());
    }

    @Test
    @WithMockUser(username = "mecanico@mottu-ops.local", roles = "MECHANIC")
    @DisplayName("PATCH /api/v1/mechanic/operations/stock/{sku} updates stock quantity")
    void shouldUpdateStockQuantity() throws Exception {
        UpdateStockRequestDto request = new UpdateStockRequestDto(6);

        mockMvc.perform(patch("/api/v1/mechanic/operations/stock/{sku}", "SKU-SPORT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-SPORT-001"))
                .andExpect(jsonPath("$.quantity").value(10)); // 4 + 6 = 10

        CatalogPartJpaEntity saved = catalogPartRepository.findByVtexSku("SKU-SPORT-001").getFirst();
        assertEquals(10, saved.getStockQuantity());
    }

    @Test
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    @DisplayName("Rider appointment creates automated WAITING_BAY service order")
    void shouldCreateAutomatedServiceOrderOnAppointment() throws Exception {
        InspectionJpaEntity inspection = new InspectionJpaEntity();
        inspection.setId(UUID.randomUUID());
        inspection.setIdempotencyKey("test-appt-auto-so");
        inspection.setContractId(testContract.getId());
        inspection.setRiderId(riderUser.getId());
        inspection.setMotorcycleId(testMoto.getId());
        inspection.setPhotoObjectKey("photos/sample.jpg");
        inspection.setPhotoSha256("sha256dummy");
        inspection.setStatus(InspectionStatus.AUTO_RESOLVED);
        inspection.setFinalVerdict(DamageVerdict.MAU_USO);
        inspection.setLaborFeeBrl(new BigDecimal("35.00"));
        inspection.setTotalEstimatedBrl(new BigDecimal("60.00"));
        inspection.setSafetyRisk(SafetyRiskLevel.SAFE_TO_RIDE);
        inspection.setReportedContext(ReportedOriginContext.NORMAL_USE_WEAR);
        inspection.setCreatedAt(Instant.now());
        inspection = inspectionRepository.save(inspection);
        final UUID targetInspectionId = inspection.getId();

        CreateAppointmentRequest apptRequest = new CreateAppointmentRequest(
                testYard.getId(),
                LocalDate.now().plusDays(2),
                AppointmentShift.MORNING
        );

        mockMvc.perform(post("/api/v1/rider/inspections/{id}/appointment", targetInspectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apptRequest)))
                .andExpect(status().isCreated());

        assertTrue(serviceOrderRepository.existsByInspectionId(targetInspectionId));
        ServiceOrderJpaEntity autoOrder = serviceOrderRepository.findAll().stream()
                .filter(o -> targetInspectionId.equals(o.getInspectionId()))
                .findFirst()
                .orElseThrow();

        assertEquals(ServiceOrderStatus.WAITING_BAY, autoOrder.getStatus());
        assertEquals("Baia 01 • Agendamento", autoOrder.getBayLabel());
        assertTrue(autoOrder.getCode().startsWith("OS-2026-"));
    }
}
