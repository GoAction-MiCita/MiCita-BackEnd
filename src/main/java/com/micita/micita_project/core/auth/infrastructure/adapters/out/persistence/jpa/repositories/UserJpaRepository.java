package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.entities.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	Optional<UserEntity> findByUsername(String username);

}
