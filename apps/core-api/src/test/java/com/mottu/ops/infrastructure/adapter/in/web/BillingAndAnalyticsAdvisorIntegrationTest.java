package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.domain.enums.*;
import com.mottu.ops.infrastructure.adapter.in.web.dto.billing.AddRiderDeliveriesRequestDto;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BillingAndAnalyticsAdvisorIntegrationTest {

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
    private SpringDataInspectionRepository inspectionRepository;

    @Autowired
    private SpringDataRiderDailyDeliveryRepository riderDailyDeliveryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserJpaEntity managerUser;
    private UserJpaEntity riderUser;
    private MotorcycleJpaEntity testMoto;
    private RentalContractJpaEntity testContract;

    @BeforeEach
    void setUp() {
        riderDailyDeliveryRepository.deleteAll();
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

        riderUser = new UserJpaEntity();
        riderUser.setId(UUID.fromString("e0000000-0000-0000-0000-000000000003"));
        riderUser.setFullName("Carlos Entregador");
        riderUser.setCpf("33344455566");
        riderUser.setPhone("11987654323");
        riderUser.setEmail("entregador@mottu-ops.local");
        riderUser.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        riderUser.setRole(UserRole.RIDER);
        riderUser.setStatus(AccountStatus.ACTIVE);
        riderUser.setCreatedAt(Instant.now());
        riderUser = userRepository.save(riderUser);

        testMoto = new MotorcycleJpaEntity(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "MOT1A23",
                MotorcycleModel.MOTTU_SPORT_110I,
                yard.getId(),
                MotorcycleStatus.RENTED,
                1500,
                Instant.now()
        );
        testMoto = motorcycleRepository.save(testMoto);

        testContract = new RentalContractJpaEntity(
                UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                riderUser.getId(),
                testMoto.getId(),
                Instant.now(),
                1000,
                null,
                true
        );
        testContract.setContractCode("CTR-2026-8841");
        testContract.setWeeklyPlanBrl(new BigDecimal("196.00"));
        testContract.setDiscountBrl(new BigDecimal("15.00"));
        testContract.setPaymentStatus("PENDING_PIX");
        testContract = rentalContractRepository.save(testContract);

        // Pre-fill with 118 deliveries over 4 days (2 away from the 120-delivery bonus!)
        riderDailyDeliveryRepository.save(new RiderDailyDeliveryJpaEntity(
                UUID.randomUUID(), riderUser.getId(), testContract.getId(), "Segunda", 30, new BigDecimal("300.00"), "98%", LocalDate.now().minusDays(3), Instant.now()
        ));
        riderDailyDeliveryRepository.save(new RiderDailyDeliveryJpaEntity(
                UUID.randomUUID(), riderUser.getId(), testContract.getId(), "Terça", 30, new BigDecimal("300.00"), "99%", LocalDate.now().minusDays(2), Instant.now()
        ));
        riderDailyDeliveryRepository.save(new RiderDailyDeliveryJpaEntity(
                UUID.randomUUID(), riderUser.getId(), testContract.getId(), "Quarta", 30, new BigDecimal("300.00"), "97%", LocalDate.now().minusDays(1), Instant.now()
        ));
        riderDailyDeliveryRepository.save(new RiderDailyDeliveryJpaEntity(
                UUID.randomUUID(), riderUser.getId(), testContract.getId(), "Quinta", 28, new BigDecimal("280.00"), "100%", LocalDate.now(), Instant.now()
        ));
    }

    @Test
    @WithMockUser(username = "gestor@mottu-ops.local", roles = "MANAGER")
    @DisplayName("GET /api/v1/manager/billing/contracts returns real contracts list")
    void shouldListBillingContracts() throws Exception {
        mockMvc.perform(get("/api/v1/manager/billing/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].contractCode").value("CTR-2026-8841"))
                .andExpect(jsonPath("$[0].riderName").value("Carlos Entregador"))
                .andExpect(jsonPath("$[0].weeklyPlanBrl").value(196.00))
                .andExpect(jsonPath("$[0].discountBrl").value(15.00))
                .andExpect(jsonPath("$[0].paymentStatus").value("PENDING_PIX"));
    }

    @Test
    @WithMockUser(username = "gestor@mottu-ops.local", roles = "MANAGER")
    @DisplayName("POST /api/v1/manager/billing/contracts/{contractCode}/confirm-pix confirms Pix payment")
    void shouldConfirmPixPaymentByManager() throws Exception {
        mockMvc.perform(post("/api/v1/manager/billing/contracts/{contractCode}/confirm-pix", "CTR-2026-8841"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractCode").value("CTR-2026-8841"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        RentalContractJpaEntity saved = rentalContractRepository.findByContractCode("CTR-2026-8841").orElseThrow();
        assertEquals("PAID", saved.getPaymentStatus());
    }

    @Test
    @WithMockUser(username = "entregador@mottu-ops.local", roles = "RIDER")
    @DisplayName("Rider rental lifecycle: summary -> add deliveries (unlock bonus R$ 40) -> pay Pix")
    void shouldExecuteRiderRentalLifecycle() throws Exception {
        // Step 1: Initial summary (118 deliveries -> bonus 0.00)
        // Net: 196.00 - 15.00 = 181.00
        mockMvc.perform(get("/api/v1/rider/rental/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractCode").value("CTR-2026-8841"))
                .andExpect(jsonPath("$.weeklyDeliveriesTotal").value(118))
                .andExpect(jsonPath("$.deliveryBonusBrl").value(0.0))
                .andExpect(jsonPath("$.weeklyTotalBrl").value(181.00))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING_PIX"));

        // Step 2: Add 5 deliveries today -> total becomes 123 (>= 120 threshold!)
        // Bonus becomes 40.00 -> Net: 196 - 15 - 40 = 141.00!
        AddRiderDeliveriesRequestDto request = new AddRiderDeliveriesRequestDto(5, new BigDecimal("55.00"));
        mockMvc.perform(post("/api/v1/rider/rental/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyDeliveriesTotal").value(123))
                .andExpect(jsonPath("$.deliveryBonusBrl").value(40.00))
                .andExpect(jsonPath("$.weeklyTotalBrl").value(141.00));

        // Step 3: Pay via Pix
        mockMvc.perform(post("/api/v1/rider/rental/pay-pix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        RentalContractJpaEntity saved = rentalContractRepository.findByContractCode("CTR-2026-8841").orElseThrow();
        assertEquals("PAID", saved.getPaymentStatus());
    }

    @Test
    @WithMockUser(username = "gestor@mottu-ops.local", roles = "MANAGER")
    @DisplayName("GET /api/v1/manager/analytics/detailed returns full operational metrics")
    void shouldReturnDetailedAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/manager/analytics/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yardSnapshots", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.dailyVolumeBars", hasSize(7)))
                .andExpect(jsonPath("$.verdictBreakdown", hasSize(3)))
                .andExpect(jsonPath("$.fleetModelBreakdown", hasSize(4)))
                .andExpect(jsonPath("$.topAuditedParts").isArray());
    }

    @Test
    @WithMockUser(username = "gestor@mottu-ops.local", roles = "MANAGER")
    @DisplayName("POST /api/v1/manager/analytics/ai-advisor returns predictive operational advice")
    void shouldReturnAiAdvisorAdvice() throws Exception {
        mockMvc.perform(post("/api/v1/manager/analytics/ai-advisor?yardKey=ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executiveHeadline").isNotEmpty())
                .andExpect(jsonPath("$.growthAnalysisText").isNotEmpty())
                .andExpect(jsonPath("$.recommendedActions", hasSize(greaterThanOrEqualTo(1))));
    }
}

