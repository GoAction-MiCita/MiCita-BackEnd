package com.micita.micita_project.core.auth.application.ports.out.query;

import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.core.auth.domain.models.User;

public interface UserQueryRepositoryPort {

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	Optional<User> findByUsername(String username);

	Optional<User> findById(UUID id);

}
