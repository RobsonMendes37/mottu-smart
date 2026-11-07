package com.mottu.ops.infrastructure.adapter.out.persistence.adapter;

import com.mottu.ops.application.port.out.UserRepositoryPort;
import com.mottu.ops.domain.enums.AccountStatus;
import com.mottu.ops.domain.model.User;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository springDataUserRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id)
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email.toLowerCase().trim())
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByCpf(String cpf) {
        return springDataUserRepository.findByCpf(cpf.trim())
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return springDataUserRepository.existsByCpf(cpf.trim());
    }

    @Override
    public List<User> findByStatus(AccountStatus status) {
        return springDataUserRepository.findByStatus(status)
                .stream()
                .map(UserPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserPersistenceMapper.toJpaEntity(user);
        UserJpaEntity saved = springDataUserRepository.save(entity);
        return UserPersistenceMapper.toDomain(saved);
    }
}
