package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.CreateMotorcycleCommand;
import com.mottu.ops.application.dto.UpdateMotorcycleStatusCommand;
import com.mottu.ops.application.dto.UploadUrlRequest;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class YardAndFleetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataYardRepository yardRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataMotorcycleRepository motorcycleRepository;

    @Autowired
    private SpringDataRentalContractRepository rentalContractRepository;

    @Autowired
    private SpringDataInspectionRepository inspectionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private YardJpaEntity yardButanta;
    private YardJpaEntity yardGuarulhos;
    private UserJpaEntity riderWithContract;
    private UserJpaEntity riderWithoutContract;
    private MotorcycleJpaEntity motoRented;
    private MotorcycleJpaEntity motoAvailable;
    private RentalContractJpaEntity activeContract;

    @BeforeEach
    void setUp() {
        inspectionRepository.deleteAll();
        rentalContractRepository.deleteAll();
        motorcycleRepository.deleteAll();
        userRepository.deleteAll();
        yardRepository.deleteAll();

        // 1. Yards
        yardButanta = new YardJpaEntity(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Galpão Mottu Butantã",
                "São Paulo",
                "Av. Corifeu de Azevedo Marques, 1000",
                true
        );
        yardButanta = yardRepository.save(yardButanta);

        yardGuarulhos = new YardJpaEntity(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Galpão Mottu Guarulhos",
                "Guarulhos",
                "Rodovia Presidente Dutra, km 220",
                true
        );
        yardGuarulhos = yardRepository.save(yardGuarulhos);

        // 2. Users
        riderWithContract = new UserJpaEntity();
        riderWithContract.setId(UUID.fromString("e0000000-0000-0000-0000-000000000003"));
        riderWithContract.setFullName("Carlos Entregador");
        riderWithContract.setCpf("33344455566");
        riderWithContract.setPhone("11987654323");
        riderWithContract.setEmail("entregador@mottu-ops.local");
        riderWithContract.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        riderWithContract.setRole(UserRole.RIDER);
        riderWithContract.setStatus(AccountStatus.ACTIVE);
        riderWithContract.setCreatedAt(Instant.now());
        riderWithContract = userRepository.save(riderWithContract);

        riderWithoutContract = new UserJpaEntity();
        riderWithoutContract.setId(UUID.fromString("e0000000-0000-0000-0000-000000000099"));
        riderWithoutContract.setFullName("Pedro Sem Moto");
        riderWithoutContract.setCpf("99944455566");
        riderWithoutContract.setPhone("11987654399");
        riderWithoutContract.setEmail("semmoto@mottu-ops.local");
        riderWithoutContract.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        riderWithoutContract.setRole(UserRole.RIDER);
        riderWithoutContract.setStatus(AccountStatus.ACTIVE);
        riderWithoutContract.setCreatedAt(Instant.now());
        riderWithoutContract = userRepository.save(riderWithoutContract);

        // 3. Motorcycles
        motoRented = new MotorcycleJpaEntity(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "MOT1A23",
                MotorcycleModel.MOTTU_SPORT_110I,
                yardButanta.getId(),
                MotorcycleStatus.RENTED,
                1000,
                Instant.now().minusSeconds(3600 * 12)
        );
        motoRented = motorcycleRepository.save(motoRented);

        motoAvailable = new MotorcycleJpaEntity(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "MOT2B34",
                MotorcycleModel.MOTTU_SPORT_110I,
                yardButanta.getId(),
                MotorcycleStatus.AVAILABLE,
                250,
                Instant.now()
        );
        motoAvailable = motorcycleRepository.save(motoAvailable);

        MotorcycleJpaEntity motoPop = new MotorcycleJpaEntity(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                "POP3C45",
                MotorcycleModel.HONDA_POP_110I,
                yardGuarulhos.getId(),
                MotorcycleStatus.AVAILABLE,
                1200,
                Instant.now()
        );
        motorcycleRepository.save(motoPop);

        // 4. Contract
        activeContract = new RentalContractJpaEntity(
                UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                riderWithContract.getId(),
                motoRented.getId(),
                Instant.now().minusSeconds(3600 * 12),
                1000,
                null,
                true
        );
        activeContract = rentalContractRepository.save(activeContract);

        // 5. Open Inspection for Carlos
        InspectionJpaEntity inspection = new InspectionJpaEntity();
        inspection.setId(UUID.randomUUID());
        inspection.setIdempotencyKey("idem-test-01");
        inspection.setContractId(activeContract.getId());
        inspection.setRiderId(riderWithContract.getId());
        inspection.setMotorcycleId(motoRented.getId());
        inspection.setPhotoObjectKey("inspections/2026/11/sample.jpg");
        inspection.setPhotoSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        inspection.setReportedContext(ReportedOriginContext.RECEIVED_LIKE_THIS);
        inspection.setSafetyRisk(SafetyRiskLevel.SAFE_TO_RIDE);
        inspection.setReportedOdometerKm(1035);
        inspection.setStatus(InspectionStatus.QUEUED_AI);
        inspection.setFinalVerdict(DamageVerdict.PRE_EXISTENTE);
        inspection.setTotalEstimatedBrl(BigDecimal.ZERO);
        inspection.setLaborFeeBrl(BigDecimal.ZERO);
        inspection.setFraudSuspected(false);
        inspection.setContestedOnce(false);
        inspection.setRetryCount(0);
        inspection.setCreatedAt(Instant.now());
        inspectionRepository.save(inspection);
    }

    // ==========================================
    // 1. YARDS (GET /api/v1/yards)
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/yards without token returns 401 Unauthorized")
    void getYardsWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/yards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/yards with authenticated RIDER returns list of active yards")
    @WithMockUser(roles = "RIDER")
    void getYardsWithRiderReturnsActiveYards() throws Exception {
        mockMvc.perform(get("/api/v1/yards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Galpão Mottu Butantã")))
                .andExpect(jsonPath("$[0].city", is("São Paulo")))
                .andExpect(jsonPath("$[1].name", is("Galpão Mottu Guarulhos")));
    }

    @Test
    @DisplayName("GET /api/v1/yards with authenticated MECHANIC returns list of active yards")
    @WithMockUser(roles = "MECHANIC")
    void getYardsWithMechanicReturnsActiveYards() throws Exception {
        mockMvc.perform(get("/api/v1/yards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ==========================================
    // 2. RIDER GARAGE (GET /api/v1/rider/garage)
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/rider/garage without auth returns 401 Unauthorized")
    void getGarageWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/rider/garage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/rider/garage with ROLE_MECHANIC returns 403 Forbidden")
    @WithMockUser(roles = "MECHANIC")
    void getGarageWithMechanicReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/rider/garage"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/rider/garage for rider with active contract returns motorcycle, yard name, open count, and recent inspections")
    @WithMockUser(username = "e0000000-0000-0000-0000-000000000003", roles = "RIDER")
    void getGarageWithContractReturnsFullOverview() throws Exception {
        mockMvc.perform(get("/api/v1/rider/garage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeContract").isMap())
                .andExpect(jsonPath("$.activeContract.contractId", is(activeContract.getId().toString())))
                .andExpect(jsonPath("$.activeContract.initialOdometerKm", is(1000)))
                .andExpect(jsonPath("$.activeContract.motorcycle.plate", is("MOT1A23")))
                .andExpect(jsonPath("$.activeContract.motorcycle.model", is("MOTTU_SPORT_110I")))
                .andExpect(jsonPath("$.activeContract.motorcycle.status", is("RENTED")))
                .andExpect(jsonPath("$.activeContract.motorcycle.yardName", is("Galpão Mottu Butantã")))
                .andExpect(jsonPath("$.openInspectionsCount", is(1)))
                .andExpect(jsonPath("$.recentInspections", hasSize(1)))
                .andExpect(jsonPath("$.recentInspections[0].status", is("QUEUED_AI")))
                .andExpect(jsonPath("$.recentInspections[0].finalVerdict", is("PRE_EXISTENTE")));
    }

    @Test
    @DisplayName("GET /api/v1/rider/garage for rider without active contract returns activeContract: null and count: 0")
    @WithMockUser(username = "e0000000-0000-0000-0000-000000000099", roles = "RIDER")
    void getGarageWithoutContractReturnsEmptyGarage() throws Exception {
        mockMvc.perform(get("/api/v1/rider/garage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeContract").value(nullValue()))
                .andExpect(jsonPath("$.openInspectionsCount", is(0)))
                .andExpect(jsonPath("$.recentInspections", empty()));
    }

    // ==========================================
    // 3. S3 UPLOAD URL (POST /api/v1/rider/inspections/upload-url)
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/rider/inspections/upload-url without auth returns 401 Unauthorized")
    void uploadUrlWithoutAuthReturns401() throws Exception {
        UploadUrlRequest request = new UploadUrlRequest("image/jpeg", 500_000L, null, null);
        mockMvc.perform(post("/api/v1/rider/inspections/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/rider/inspections/upload-url with invalid MIME returns 400 Bad Request")
    @WithMockUser(roles = "RIDER")
    void uploadUrlInvalidMimeReturns400() throws Exception {
        UploadUrlRequest request = new UploadUrlRequest("application/pdf", 100_000L, null, null);
        mockMvc.perform(post("/api/v1/rider/inspections/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Tipo de arquivo inválido")));
    }

    @Test
    @DisplayName("POST /api/v1/rider/inspections/upload-url with size > 10MB returns 400 Bad Request")
    @WithMockUser(roles = "RIDER")
    void uploadUrlOversizedReturns400() throws Exception {
        UploadUrlRequest request = new UploadUrlRequest("image/jpeg", 15_000_000L, null, null);
        mockMvc.perform(post("/api/v1/rider/inspections/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/rider/inspections/upload-url with valid request returns presigned PUT URL and object key")
    @WithMockUser(roles = "RIDER")
    void uploadUrlValidReturnsPresignedPutUrl() throws Exception {
        UUID inspectionId = UUID.randomUUID();
        UploadUrlRequest request = new UploadUrlRequest("image/webp", 350_000L, inspectionId, "front_wheel");

        mockMvc.perform(post("/api/v1/rider/inspections/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl", containsString("http://localhost:19000")))
                .andExpect(jsonPath("$.uploadUrl", containsString("X-Amz-Signature=")))
                .andExpect(jsonPath("$.photoObjectKey", containsString("inspections/" + inspectionId + "/front_wheel_")))
                .andExpect(jsonPath("$.photoObjectKey", endsWith(".webp")))
                .andExpect(jsonPath("$.expiresInSeconds", is(900)));
    }

    @Test
    @DisplayName("POST /api/v1/rider/upload-url alias route works identically")
    @WithMockUser(roles = "RIDER")
    void uploadUrlAliasRouteSuccess() throws Exception {
        UploadUrlRequest request = new UploadUrlRequest("image/jpeg", 200_000L, null, null);

        mockMvc.perform(post("/api/v1/rider/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl", containsString("http://localhost:19000")))
                .andExpect(jsonPath("$.photoObjectKey", startsWith("inspections/")))
                .andExpect(jsonPath("$.expiresInSeconds", is(900)));
    }

    // ==========================================
    // 4. FLEET MANAGEMENT (GET/POST /api/v1/fleet/motorcycles)
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/fleet/motorcycles without auth returns 401 Unauthorized")
    void fleetListWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/motorcycles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/fleet/motorcycles with ROLE_RIDER returns 403 Forbidden")
    @WithMockUser(roles = "RIDER")
    void fleetListWithRiderReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/motorcycles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/fleet/motorcycles with ROLE_MECHANIC returns PageResponse with activeRiderName")
    @WithMockUser(roles = "MECHANIC")
    void fleetListWithMechanicReturnsPageResponse() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/motorcycles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/fleet/motorcycles filtering by plate substring resolves activeRiderName")
    @WithMockUser(roles = "MANAGER")
    void fleetListFilterByPlateResolvesRiderName() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/motorcycles?plate=mot1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].plate", is("MOT1A23")))
                .andExpect(jsonPath("$.items[0].status", is("RENTED")))
                .andExpect(jsonPath("$.items[0].activeRiderName", is("Carlos Entregador")))
                .andExpect(jsonPath("$.items[0].yardName", is("Galpão Mottu Butantã")));
    }

    @Test
    @DisplayName("GET /api/v1/fleet/motorcycles filtering by AVAILABLE status returns unrented motorcycles with null rider")
    @WithMockUser(roles = "MECHANIC")
    void fleetListFilterByStatusAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/motorcycles?status=AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].status", everyItem(is("AVAILABLE"))))
                .andExpect(jsonPath("$.items[0].activeRiderName").value(nullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/fleet/motorcycles with ROLE_MANAGER registers new AVAILABLE motorcycle")
    @WithMockUser(roles = "MANAGER")
    void fleetCreateMotorcycleSuccess() throws Exception {
        CreateMotorcycleCommand command = new CreateMotorcycleCommand(
                "TST9Z99",
                MotorcycleModel.MOTTU_SPORT_110I,
                yardButanta.getId(),
                0
        );

        mockMvc.perform(post("/api/v1/fleet/motorcycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.plate", is("TST9Z99")))
                .andExpect(jsonPath("$.model", is("MOTTU_SPORT_110I")))
                .andExpect(jsonPath("$.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.yardName", is("Galpão Mottu Butantã")))
                .andExpect(jsonPath("$.currentOdometerKm", is(0)));
    }

    @Test
    @DisplayName("POST /api/v1/fleet/motorcycles with duplicate plate returns 409 Conflict")
    @WithMockUser(roles = "MANAGER")
    void fleetCreateDuplicatePlateReturns409() throws Exception {
        CreateMotorcycleCommand command = new CreateMotorcycleCommand(
                "MOT1A23",
                MotorcycleModel.MOTTU_SPORT_110I,
                yardButanta.getId(),
                0
        );

        mockMvc.perform(post("/api/v1/fleet/motorcycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/fleet/motorcycles with nonexistent yard returns 404 Not Found")
    @WithMockUser(roles = "MANAGER")
    void fleetCreateNonexistentYardReturns404() throws Exception {
        CreateMotorcycleCommand command = new CreateMotorcycleCommand(
                "NEW9999",
                MotorcycleModel.MOTTU_SPORT_110I,
                UUID.randomUUID(),
                0
        );

        mockMvc.perform(post("/api/v1/fleet/motorcycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/fleet/motorcycles/{id} returns details for existing motorcycle")
    @WithMockUser(roles = "MECHANIC")
    void fleetGetByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/motorcycles/{id}", motoRented.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(motoRented.getId().toString())))
                .andExpect(jsonPath("$.plate", is("MOT1A23")))
                .andExpect(jsonPath("$.status", is("RENTED")))
                .andExpect(jsonPath("$.activeRiderName", is("Carlos Entregador")));
    }

    @Test
    @DisplayName("GET /api/v1/fleet/motorcycles/{id} for nonexistent UUID returns 404 Not Found")
    @WithMockUser(roles = "MECHANIC")
    void fleetGetByIdNonexistentReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/motorcycles/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/fleet/motorcycles/{id}/status transitions AVAILABLE to IN_MAINTENANCE")
    @WithMockUser(roles = "MECHANIC")
    void fleetUpdateStatusToMaintenanceSuccess() throws Exception {
        UpdateMotorcycleStatusCommand command = new UpdateMotorcycleStatusCommand(MotorcycleStatus.IN_MAINTENANCE);

        mockMvc.perform(patch("/api/v1/fleet/motorcycles/{id}/status", motoAvailable.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(motoAvailable.getId().toString())))
                .andExpect(jsonPath("$.status", is("IN_MAINTENANCE")));
    }

    @Test
    @DisplayName("PATCH /api/v1/fleet/motorcycles/{id}/status transitions IN_MAINTENANCE back to AVAILABLE")
    @WithMockUser(roles = "MECHANIC")
    void fleetUpdateStatusBackToAvailableSuccess() throws Exception {
        // First put into maintenance
        motoAvailable.setStatus(MotorcycleStatus.IN_MAINTENANCE);
        motorcycleRepository.save(motoAvailable);

        UpdateMotorcycleStatusCommand command = new UpdateMotorcycleStatusCommand(MotorcycleStatus.AVAILABLE);

        mockMvc.perform(patch("/api/v1/fleet/motorcycles/{id}/status", motoAvailable.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    @DisplayName("PATCH /api/v1/fleet/motorcycles/{id}/status on RENTED motorcycle returns 422 Unprocessable Entity")
    @WithMockUser(roles = "MECHANIC")
    void fleetUpdateStatusRentedMotorcycleReturns422() throws Exception {
        UpdateMotorcycleStatusCommand command = new UpdateMotorcycleStatusCommand(MotorcycleStatus.IN_MAINTENANCE);

        mockMvc.perform(patch("/api/v1/fleet/motorcycles/{id}/status", motoRented.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("Motocicleta alugada não pode ter o status alterado")));
    }
}
