package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.application.dto.RecordBaseline360Command;
import com.mottu.ops.application.dto.ResolveInspectionRequest;
import com.mottu.ops.domain.enums.DamageVerdict;
import com.mottu.ops.domain.enums.InspectionStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.enums.ReportedOriginContext;
import com.mottu.ops.domain.enums.SafetyRiskLevel;
import com.mottu.ops.domain.enums.ServiceOrderStatus;
import com.mottu.ops.domain.enums.UserRole;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.*;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.*;
import com.mottu.ops.infrastructure.adapter.out.security.JwtTokenProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Baseline360AndCommitteeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProviderAdapter jwtTokenProvider;

    @Autowired
    private SpringDataRentalContractRepository rentalContractRepository;

    @Autowired
    private SpringDataInspectionRepository inspectionRepository;

    @Autowired
    private SpringDataServiceOrderRepository serviceOrderRepository;

    @Autowired
    private SpringDataMotorcycleRepository motorcycleRepository;

    @Autowired
    private SpringDataYardRepository yardRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    private static final UUID YARD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MECHANIC_ID = UUID.fromString("e0000000-0000-0000-0000-000000000002");
    private static final UUID RIDER_ID = UUID.fromString("e0000000-0000-0000-0000-000000000003");
    private static final UUID MOTO_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CONTRACT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        serviceOrderRepository.deleteAll();
        inspectionRepository.deleteAll();
        rentalContractRepository.deleteAll();
        motorcycleRepository.deleteAll();
        userRepository.deleteAll();
        yardRepository.deleteAll();

        YardJpaEntity yard = new YardJpaEntity(YARD_ID, "Galpão Central", "São Paulo", "Av. Paulista, 100", true);
        yardRepository.save(yard);

        UserJpaEntity mechanic = new UserJpaEntity();
        mechanic.setId(MECHANIC_ID);
        mechanic.setFullName("Mecânico Chefe");
        mechanic.setCpf("22233344455");
        mechanic.setPhone("11987654322");
        mechanic.setEmail("mecanico@mottu.com");
        mechanic.setPasswordHash("hash");
        mechanic.setRole(UserRole.MECHANIC);
        mechanic.setStatus(com.mottu.ops.domain.enums.AccountStatus.ACTIVE);
        mechanic.setCreatedAt(Instant.now());
        userRepository.save(mechanic);

        UserJpaEntity rider = new UserJpaEntity();
        rider.setId(RIDER_ID);
        rider.setFullName("Carlos Entregador");
        rider.setCpf("33344455566");
        rider.setPhone("11987654323");
        rider.setEmail("entregador@mottu.com");
        rider.setPasswordHash("hash");
        rider.setRole(UserRole.RIDER);
        rider.setStatus(com.mottu.ops.domain.enums.AccountStatus.ACTIVE);
        rider.setCreatedAt(Instant.now());
        userRepository.save(rider);

        MotorcycleJpaEntity moto = new MotorcycleJpaEntity(
                MOTO_ID, "MOT1A23", MotorcycleModel.MOTTU_SPORT_110I, YARD_ID, MotorcycleStatus.RENTED, 1000, Instant.now()
        );
        motorcycleRepository.save(moto);

        RentalContractJpaEntity contract = new RentalContractJpaEntity(
                CONTRACT_ID, RIDER_ID, MOTO_ID, Instant.now().minusSeconds(3600), 1000, null, true
        );
        contract.setContractCode("CTR-TEST-001");
        rentalContractRepository.save(contract);
    }

    @Test
    @DisplayName("Mecânico deve registrar vistoria 360 no pátio e entregador deve validar na garagem com escudo ativo")
    void shouldRecordBaseline360AndRiderValidateWithActiveShield() throws Exception {
        String mechanicToken = jwtTokenProvider.generateToken(MECHANIC_ID, "mecanico@mottu.com", UserRole.MECHANIC, "Mecânico");
        String riderToken = jwtTokenProvider.generateToken(RIDER_ID, "entregador@mottu.com", UserRole.RIDER, "Entregador");

        // 1. Mecânico registra giro 360 na expedição
        RecordBaseline360Command command = new RecordBaseline360Command(
                "/mottu-inspections/baseline_front.jpg",
                "/mottu-inspections/baseline_rear.jpg",
                "/mottu-inspections/baseline_right.jpg",
                "/mottu-inspections/baseline_left.jpg",
                "/mottu-inspections/baseline_odo.jpg",
                1000
        );

        mockMvc.perform(post("/api/v1/fleet/motorcycles/" + MOTO_ID + "/baseline-360")
                        .header("Authorization", "Bearer " + mechanicToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupPhotosJson").isNotEmpty());

        // 2. Entregador acessa Garagem e vê o giro 360 + Escudo 48h/100km
        mockMvc.perform(get("/api/v1/rider/garage")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeContract.pickupPhotosJson").isNotEmpty())
                .andExpect(jsonPath("$.activeContract.isShieldActive").value(true));

        // 3. Entregador assina digitalmente a dupla validação
        mockMvc.perform(post("/api/v1/rider/contracts/validate-pickup")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupValidatedByRider").value(true))
                .andExpect(jsonPath("$.pickupValidatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Mecânico pode convocar para diagnóstico presencial na bancada T06 gerando OS automática nas baias")
    void shouldConvokePresentialInspectionAndAutoCreateServiceOrder() throws Exception {
        String mechanicToken = jwtTokenProvider.generateToken(MECHANIC_ID, "mecanico@mottu.com", UserRole.MECHANIC, "Mecânico");

        // Create an inspection in HUMAN_TRIAGE
        UUID inspectionId = UUID.randomUUID();
        RentalContractJpaEntity contract = rentalContractRepository.findByRiderIdAndActiveTrue(RIDER_ID).orElseThrow();

        InspectionJpaEntity inspection = new InspectionJpaEntity();
        inspection.setId(inspectionId);
        inspection.setIdempotencyKey("idemp-" + inspectionId);
        inspection.setContractId(contract.getId());
        inspection.setRiderId(RIDER_ID);
        inspection.setMotorcycleId(contract.getMotorcycleId());
        inspection.setPhotoObjectKey("test_damage.jpg");
        inspection.setPhotoSha256("sha256-test-" + inspectionId);
        inspection.setReportedContext(ReportedOriginContext.NORMAL_USE_WEAR);
        inspection.setSafetyRisk(SafetyRiskLevel.SAFE_TO_RIDE);
        inspection.setReportedOdometerKm(1100);
        inspection.setStatus(InspectionStatus.HUMAN_TRIAGE);
        inspection.setCreatedAt(Instant.now());
        inspectionRepository.save(inspection);

        // Resolve with requestPresentialInspection = true
        ResolveInspectionRequest request = new ResolveInspectionRequest(
                DamageVerdict.DESGASTE_NATURAL,
                List.of(),
                "Ruído metálico no cabeçote impossível de aferir por foto. Solicito presença na baia 01.",
                true
        );

        mockMvc.perform(post("/api/v1/mechanic/inspections/" + inspectionId + "/resolve")
                        .header("Authorization", "Bearer " + mechanicToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HUMAN_RESOLVED"))
                .andExpect(jsonPath("$.totalEstimatedBrl").value(0.00))
                .andExpect(jsonPath("$.customerFeedbackText").value(org.hamcrest.Matchers.containsString("presencial gratuita")));

        // Verify that inspection is marked presential
        InspectionJpaEntity savedInspection = inspectionRepository.findById(inspectionId).orElseThrow();
        assertThat(savedInspection.isPresentialInspectionRequested()).isTrue();
        assertThat(savedInspection.getTotalEstimatedBrl()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify that Service Order in bay WMS was auto-created
        List<ServiceOrderJpaEntity> orders = serviceOrderRepository.findAllByOrderByCreatedAtDesc();
        boolean osCreated = orders.stream().anyMatch(o -> inspectionId.equals(o.getInspectionId()) &&
                o.getOriginLabel().contains("DIAGNÓSTICO PRESENCIAL") &&
                o.getStatus() == ServiceOrderStatus.WAITING_BAY);
        assertThat(osCreated).isTrue();
    }
}
