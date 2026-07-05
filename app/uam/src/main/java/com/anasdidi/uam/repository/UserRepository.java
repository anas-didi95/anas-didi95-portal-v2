package com.anasdidi.uam.repository;

import com.anasdidi.uam.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository
    extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

  Optional<UserEntity> findByUsername(String username);

  Optional<UserEntity> findByIdAndVersion(UUID id, Integer version);
}
