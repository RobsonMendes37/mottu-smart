package com.mottu.ops.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.enums.MotorcycleModel;
import com.mottu.ops.domain.enums.MotorcycleStatus;
import com.mottu.ops.domain.enums.UserRole;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.MotorcycleJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.RentalContractJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.YardJpaEntity;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthAndApprovalIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private UUID managerId;
    private String managerToken;
    private YardJpaEntity defaultYard;

    @BeforeEach
    void setUp() throws Exception {
        rentalContractRepository.deleteAll();
        motorcycleRepository.deleteAll();
        userRepository.deleteAll();
        yardRepository.deleteAll();

        // Seed default yard
        defaultYard = new YardJpaEntity();
        defaultYard.setId(UUID.randomUUID());
        defaultYard.setName("Galpão Central");
        defaultYard.setCity("São Paulo");
        defaultYard.setAddress("Rua das Oficinas, 100");
        defaultYard.setActive(true);
        defaultYard = yardRepository.save(defaultYard);

        // Seed seed manager user
        UserJpaEntity manager = new UserJpaEntity();
        manager.setId(UUID.fromString("e0000000-0000-0000-0000-000000000001"));
        manager.setFullName("Gestor Operações");
        manager.setCpf("00000000001");
        manager.setPhone("11988880001");
        manager.setEmail("gestor@mottu-ops.local");
        // Hash for Mottu@2026!
        manager.setPasswordHash(passwordEncoder.encode("Mottu@2026!"));
        manager.setRole(UserRole.MANAGER);
        manager.setStatus(AccountStatus.ACTIVE);
        manager.setCreatedAt(Instant.now());
        manager = userRepository.save(manager);
        managerId = manager.getId();

        // Login as manager to obtain manager token
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "gestor@mottu-ops.local",
                                "password", "Mottu@2026!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        managerToken = loginJson.get("accessToken").asText();
    }

    @Test
    @DisplayName("1. GET /api/v1/auth/public-config should return configured publicDemoMode")
    void publicConfig_shouldReturnConfiguredDemoModeFlag() throws Exception {
        mockMvc.perform(get("/api/v1/auth/public-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicDemoMode").value(false));
    }

    @Test
    @DisplayName("2. POST /api/v1/auth/register with valid RIDER creates user with PENDING_APPROVAL and BCrypt password")
    void register_withValidRider_shouldCreateUserInPendingApprovalWithBcryptPassword() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "João Silva",
                "cpf", "12345678901",
                "phone", "11999990000",
                "email", "joao@email.com",
                "password", "Senha@123",
                "role", "RIDER"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.message").value("Cadastro recebido! Aguarde a liberação do Gestor."))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID createdUserId = UUID.fromString(responseNode.get("userId").asText());

        Optional<UserJpaEntity> createdUser = userRepository.findById(createdUserId);
        assertThat(createdUser).isPresent();
        assertThat(createdUser.get().getStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(createdUser.get().getRole()).isEqualTo(UserRole.RIDER);
        assertThat(createdUser.get().getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("Senha@123", createdUser.get().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("3. POST /api/v1/auth/register with MANAGER role should be rejected with 400 Bad Request")
    void register_withManagerRole_shouldRejectWithBadRequest400() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "Gestor Infiltrado",
                "cpf", "98765432100",
                "phone", "11988887777",
                "email", "fakegestor@mottu-ops.local",
                "password", "Mottu@2026!",
                "role", "MANAGER"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("MANAGER")));
    }

    @Test
    @DisplayName("4. POST /api/v1/auth/register with duplicate CPF or Email should reject with 409 Conflict")
    void register_withDuplicateCpfOrEmail_shouldRejectWithConflict409() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "Carlos Entregador",
                "cpf", "11122233344",
                "phone", "11977776666",
                "email", "carlos@email.com",
                "password", "Mottu@2026!",
                "role", "RIDER"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Try registering again with same email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("5. POST /api/v1/auth/login with PENDING_APPROVAL user should return 403 Forbidden with RFC 7807")
    void login_withPendingUser_shouldReturn403ForbiddenWithStatusDetail() throws Exception {
        Map<String, Object> body = Map.of(
                "fullName", "Rider Pendente",
                "cpf", "22233344455",
                "phone", "11966665555",
                "email", "pendente@email.com",
                "password", "Senha@123",
                "role", "RIDER"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "pendente@email.com",
                                "password", "Senha@123"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Proibido"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Conta aguardando aprovação do Gestor."));
    }

    @Test
    @DisplayName("6. POST /api/v1/auth/login with REJECTED user should return 403 Forbidden with rejection reason")
    void login_withRejectedUser_shouldReturn403ForbiddenWithRejectionReason() throws Exception {
        // Create user in rejected state
        UserJpaEntity rejectedUser = new UserJpaEntity();
        rejectedUser.setId(UUID.randomUUID());
        rejectedUser.setFullName("Rider Reprovado");
        rejectedUser.setCpf("33344455566");
        rejectedUser.setPhone("11955554444");
        rejectedUser.setEmail("reprovado@email.com");
        rejectedUser.setPasswordHash(passwordEncoder.encode("Senha@123"));
        rejectedUser.setRole(UserRole.RIDER);
        rejectedUser.setStatus(AccountStatus.REJECTED);
        rejectedUser.setRejectionReason("CPF com divergência cadastral");
        rejectedUser.setApprovedBy(managerId);
        rejectedUser.setCreatedAt(Instant.now());
        userRepository.save(rejectedUser);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "reprovado@email.com",
                                "password", "Senha@123"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value(containsString("CPF com divergência cadastral")));
    }

    @Test
    @DisplayName("7. POST /api/v1/auth/login with ACTIVE user returns 200 OK and valid JWT token")
    void login_withActiveUser_shouldReturnAccessTokenAndRoleAndFullName() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "gestor@mottu-ops.local",
                                "password", "Mottu@2026!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.fullName").value("Gestor Operações"))
                .andExpect(jsonPath("$.userId").value(managerId.toString()))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(responseNode.get("accessToken").asText()).isNotEmpty();
    }

    @Test
    @DisplayName("8. POST /api/v1/auth/login with wrong password returns 401 Unauthorized")
    void login_withInvalidPassword_shouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "gestor@mottu-ops.local",
                                "password", "SenhaErrada!2026"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("9. GET /api/v1/manager/users/pending returns list of pending users")
    void getPendingUsers_asManager_shouldReturnListOfPendingUsers() throws Exception {
        // Create 2 pending users
        UserJpaEntity u1 = new UserJpaEntity();
        u1.setId(UUID.randomUUID());
        u1.setFullName("Candidato 1");
        u1.setCpf("44455566677");
        u1.setPhone("11944443333");
        u1.setEmail("candidato1@mottu-ops.local");
        u1.setPasswordHash(passwordEncoder.encode("Senha@123"));
        u1.setRole(UserRole.RIDER);
        u1.setStatus(AccountStatus.PENDING_APPROVAL);
        u1.setCreatedAt(Instant.now());
        userRepository.save(u1);

        UserJpaEntity u2 = new UserJpaEntity();
        u2.setId(UUID.randomUUID());
        u2.setFullName("Candidato Mecanico");
        u2.setCpf("55566677788");
        u2.setPhone("11933332222");
        u2.setEmail("mecanico1@mottu-ops.local");
        u2.setPasswordHash(passwordEncoder.encode("Senha@123"));
        u2.setRole(UserRole.MECHANIC);
        u2.setStatus(AccountStatus.PENDING_APPROVAL);
        u2.setCreatedAt(Instant.now());
        userRepository.save(u2);

        mockMvc.perform(get("/api/v1/manager/users/pending")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fullName").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @DisplayName("10. POST /api/v1/manager/users/{id}/approve for RIDER activates user, sets motorcycle to RENTED, and creates RentalContract")
    void approveRider_asManager_shouldActivateUserChangeMotorcycleToRentedAndCreateActiveContract() throws Exception {
        // Create pending rider
        UserJpaEntity rider = new UserJpaEntity();
        rider.setId(UUID.randomUUID());
        rider.setFullName("Piloto Aprovado");
        rider.setCpf("66677788899");
        rider.setPhone("11922221111");
        rider.setEmail("piloto@mottu-ops.local");
        rider.setPasswordHash(passwordEncoder.encode("Senha@123"));
        rider.setRole(UserRole.RIDER);
        rider.setStatus(AccountStatus.PENDING_APPROVAL);
        rider.setCreatedAt(Instant.now());
        rider = userRepository.save(rider);

        // Create available motorcycle
        MotorcycleJpaEntity moto = new MotorcycleJpaEntity();
        moto.setId(UUID.randomUUID());
        moto.setPlate("MOT2B34");
        moto.setModel(MotorcycleModel.MOTTU_SPORT_110I);
        moto.setYardId(defaultYard.getId());
        moto.setStatus(MotorcycleStatus.AVAILABLE);
        moto.setCurrentOdometerKm(1200);
        moto.setCreatedAt(Instant.now());
        moto = motorcycleRepository.save(moto);

        MvcResult result = mockMvc.perform(post("/api/v1/manager/users/{id}/approve", rider.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "motorcycleId", moto.getId().toString()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(rider.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.contractId").isNotEmpty())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID contractId = UUID.fromString(responseNode.get("contractId").asText());

        // Verify DB updates
        UserJpaEntity updatedUser = userRepository.findById(rider.getId()).orElseThrow();
        assertThat(updatedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(updatedUser.getApprovedBy()).isEqualTo(managerId);

        MotorcycleJpaEntity updatedMoto = motorcycleRepository.findById(moto.getId()).orElseThrow();
        assertThat(updatedMoto.getStatus()).isEqualTo(MotorcycleStatus.RENTED);

        Optional<RentalContractJpaEntity> contract = rentalContractRepository.findById(contractId);
        assertThat(contract).isPresent();
        assertThat(contract.get().getRiderId()).isEqualTo(rider.getId());
        assertThat(contract.get().getMotorcycleId()).isEqualTo(moto.getId());
        assertThat(contract.get().getInitialOdometerKm()).isEqualTo(1200);
        assertThat(contract.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("11. POST /api/v1/manager/users/{id}/approve without motorcycleId for RIDER returns 400 Bad Request")
    void approveRider_withoutMotorcycleId_shouldReturn400BadRequest() throws Exception {
        UserJpaEntity rider = new UserJpaEntity();
        rider.setId(UUID.randomUUID());
        rider.setFullName("Piloto Sem Moto");
        rider.setCpf("77788899900");
        rider.setPhone("11911112222");
        rider.setEmail("semmoto@mottu-ops.local");
        rider.setPasswordHash(passwordEncoder.encode("Senha@123"));
        rider.setRole(UserRole.RIDER);
        rider.setStatus(AccountStatus.PENDING_APPROVAL);
        rider.setCreatedAt(Instant.now());
        rider = userRepository.save(rider);

        mockMvc.perform(post("/api/v1/manager/users/{id}/approve", rider.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("motorcycleId")));
    }

    @Test
    @DisplayName("12. POST /api/v1/manager/users/{id}/approve with already RENTED motorcycle returns 422 Unprocessable")
    void approveRider_withAlreadyRentedMotorcycle_shouldReturn422UnprocessableEntity() throws Exception {
        UserJpaEntity rider = new UserJpaEntity();
        rider.setId(UUID.randomUUID());
        rider.setFullName("Piloto Conflito");
        rider.setCpf("88899900011");
        rider.setPhone("11912345678");
        rider.setEmail("conflito@mottu-ops.local");
        rider.setPasswordHash(passwordEncoder.encode("Senha@123"));
        rider.setRole(UserRole.RIDER);
        rider.setStatus(AccountStatus.PENDING_APPROVAL);
        rider.setCreatedAt(Instant.now());
        rider = userRepository.save(rider);

        MotorcycleJpaEntity rentedMoto = new MotorcycleJpaEntity();
        rentedMoto.setId(UUID.randomUUID());
        rentedMoto.setPlate("RENTED1");
        rentedMoto.setModel(MotorcycleModel.MOTTU_SPORT_110I);
        rentedMoto.setYardId(defaultYard.getId());
        rentedMoto.setStatus(MotorcycleStatus.RENTED);
        rentedMoto.setCurrentOdometerKm(5000);
        rentedMoto.setCreatedAt(Instant.now());
        rentedMoto = motorcycleRepository.save(rentedMoto);

        mockMvc.perform(post("/api/v1/manager/users/{id}/approve", rider.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "motorcycleId", rentedMoto.getId().toString()
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("13. POST /api/v1/manager/users/{id}/approve for MECHANIC activates user without contract")
    void approveMechanic_asManager_shouldActivateWithoutContract() throws Exception {
        UserJpaEntity mechanic = new UserJpaEntity();
        mechanic.setId(UUID.randomUUID());
        mechanic.setFullName("Mecânico Oficina");
        mechanic.setCpf("99900011122");
        mechanic.setPhone("11987654321");
        mechanic.setEmail("oficina@mottu-ops.local");
        mechanic.setPasswordHash(passwordEncoder.encode("Senha@123"));
        mechanic.setRole(UserRole.MECHANIC);
        mechanic.setStatus(AccountStatus.PENDING_APPROVAL);
        mechanic.setCreatedAt(Instant.now());
        mechanic = userRepository.save(mechanic);

        mockMvc.perform(post("/api/v1/manager/users/{id}/approve", mechanic.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(mechanic.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.contractId").value(nullValue()));

        UserJpaEntity updated = userRepository.findById(mechanic.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(updated.getApprovedBy()).isEqualTo(managerId);
    }

    @Test
    @DisplayName("14. POST /api/v1/manager/users/{id}/reject sets REJECTED status with reason")
    void rejectUser_asManager_shouldSetStatusRejectedWithReason() throws Exception {
        UserJpaEntity candidate = new UserJpaEntity();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Candidato Reprovado");
        candidate.setCpf("12398745600");
        candidate.setPhone("11955556666");
        candidate.setEmail("reprovar@mottu-ops.local");
        candidate.setPasswordHash(passwordEncoder.encode("Senha@123"));
        candidate.setRole(UserRole.RIDER);
        candidate.setStatus(AccountStatus.PENDING_APPROVAL);
        candidate.setCreatedAt(Instant.now());
        candidate = userRepository.save(candidate);

        mockMvc.perform(post("/api/v1/manager/users/{id}/reject", candidate.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "CNH inválida ou vencida."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(candidate.getId().toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        UserJpaEntity updated = userRepository.findById(candidate.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.REJECTED);
        assertThat(updated.getRejectionReason()).isEqualTo("CNH inválida ou vencida.");
        assertThat(updated.getApprovedBy()).isEqualTo(managerId);
    }

    @Test
    @DisplayName("15. GET /api/v1/auth/me returns current user profile for authenticated user")
    void me_asAuthenticatedUser_shouldReturnCurrentUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(managerId.toString()))
                .andExpect(jsonPath("$.fullName").value("Gestor Operações"))
                .andExpect(jsonPath("$.email").value("gestor@mottu-ops.local"))
                .andExpect(jsonPath("$.cpf").value("00000000001"))
                .andExpect(jsonPath("$.phone").value("11988880001"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
