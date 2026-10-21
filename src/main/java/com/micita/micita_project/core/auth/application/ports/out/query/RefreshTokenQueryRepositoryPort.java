package com.micita.micita_project.core.auth.application.ports.out.query;

import java.util.Optional;

import com.micita.micita_project.core.auth.domain.models.RefreshToken;

public interface RefreshTokenQueryRepositoryPort {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

}
