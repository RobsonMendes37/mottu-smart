package com.mottu.ops.application.port.out;

import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByCpf(String cpf);
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
    List<User> findByStatus(AccountStatus status);
    User save(User user);
}
