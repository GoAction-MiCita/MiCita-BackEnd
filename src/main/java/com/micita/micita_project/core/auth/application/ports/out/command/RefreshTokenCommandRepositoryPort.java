package com.micita.micita_project.core.auth.application.ports.out.command;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.auth.domain.models.RefreshToken;

public interface RefreshTokenCommandRepositoryPort {

	/**
	 * @return the stored token, with its generated id
	 */
	RefreshToken save(RefreshToken refreshToken);

	/**
	 * Revokes the token unless it was already revoked.
	 * @return {@code true} if this call revoked it, {@code false} if it was already revoked or
	 * does not exist
	 */
	boolean revoke(UUID id, Instant revokedAt);

}
