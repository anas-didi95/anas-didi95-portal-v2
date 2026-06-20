package com.anasdidi.uam.repository;

import com.anasdidi.uam.entity.UserEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {}
