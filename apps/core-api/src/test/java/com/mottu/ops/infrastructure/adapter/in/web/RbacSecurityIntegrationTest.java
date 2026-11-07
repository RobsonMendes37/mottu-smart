package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.UserRole;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.mottu.ops.infrastructure.adapter.out.security.JwtTokenProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RbacSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProviderAdapter tokenProvider;

    private String managerToken;
    private String riderToken;
    private String mechanicToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // 1. Manager user
        UserJpaEntity manager = new UserJpaEntity();
        manager.setId(UUID.fromString("e0000000-0000-0000-0000-000000000001"));
        manager.setFullName("Gestor Operações");
        manager.setCpf("00000000001");
        manager.setPhone("11988880001");
        manager.setEmail("gestor@mottu-ops.local");
        manager.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        manager.setRole(UserRole.MANAGER);
        manager.setStatus(AccountStatus.ACTIVE);
        manager.setCreatedAt(Instant.now());
        userRepository.save(manager);
        managerToken = tokenProvider.generateToken(manager.getId(), manager.getEmail(), manager.getRole(), manager.getFullName());

        // 2. Mechanic user
        UserJpaEntity mechanic = new UserJpaEntity();
        mechanic.setId(UUID.fromString("e0000000-0000-0000-0000-000000000002"));
        mechanic.setFullName("Mecânico Silva");
        mechanic.setCpf("00000000002");
        mechanic.setPhone("11988880002");
        mechanic.setEmail("mecanico@mottu-ops.local");
        mechanic.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        mechanic.setRole(UserRole.MECHANIC);
        mechanic.setStatus(AccountStatus.ACTIVE);
        mechanic.setCreatedAt(Instant.now());
        userRepository.save(mechanic);
        mechanicToken = tokenProvider.generateToken(mechanic.getId(), mechanic.getEmail(), mechanic.getRole(), mechanic.getFullName());

        // 3. Rider user
        UserJpaEntity rider = new UserJpaEntity();
        rider.setId(UUID.fromString("e0000000-0000-0000-0000-000000000003"));
        rider.setFullName("Carlos Entregador");
        rider.setCpf("00000000003");
        rider.setPhone("11988880003");
        rider.setEmail("entregador@mottu-ops.local");
        rider.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        rider.setRole(UserRole.RIDER);
        rider.setStatus(AccountStatus.ACTIVE);
        rider.setCreatedAt(Instant.now());
        userRepository.save(rider);
        riderToken = tokenProvider.generateToken(rider.getId(), rider.getEmail(), rider.getRole(), rider.getFullName());
    }

    @Test
    @DisplayName("1. Unauthenticated requests to protected endpoints return 401 Unauthorized")
    void unauthenticatedAccess_toProtectedEndpoints_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/manager/users/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/rider/garage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/mechanic/queue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("2. RIDER token accessing /api/v1/manager/** routes returns 403 Forbidden")
    void riderToken_accessingManagerRoutes_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/manager/users/pending")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/v1/manager/users/" + UUID.randomUUID() + "/approve")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/v1/manager/users/" + UUID.randomUUID() + "/reject")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"qualquer\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("3. RIDER token accessing /api/v1/mechanic/** routes returns 403 Forbidden")
    void riderToken_accessingMechanicRoutes_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/mechanic/queue")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("4. MECHANIC token accessing /api/v1/manager/** routes returns 403 Forbidden")
    void mechanicToken_accessingManagerRoutes_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/manager/users/pending")
                        .header("Authorization", "Bearer " + mechanicToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("5. MECHANIC token accessing /api/v1/rider/** routes returns 403 Forbidden")
    void mechanicToken_accessingRiderRoutes_shouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/rider/garage")
                        .header("Authorization", "Bearer " + mechanicToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("6. MANAGER token accessing /api/v1/manager/users/pending returns 200 OK")
    void managerToken_accessingManagerRoutes_shouldSucceed200() throws Exception {
        mockMvc.perform(get("/api/v1/manager/users/pending")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("7. Malformed or tampered token returns 401 Unauthorized")
    void invalidOrMalformedJwtToken_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-garbage-token-123456")) // nosec
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("8. Expired JWT token returns 401 Unauthorized")
    void expiredJwtToken_shouldReturn401Unauthorized() throws Exception {
        // Generate an expired token (negative expiration)
        String expiredToken = tokenProvider.generateExpiredToken(
                UUID.randomUUID(), "expired@mottu.local", UserRole.RIDER, "Expirado"
        );

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
