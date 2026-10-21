package com.micita.micita_project.core.auth.domain.models;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A stored refresh token. Only the hash of the token value is kept; the raw value is handed to the
 * client once. A {@code null} id means it has not been persisted yet, and a {@code null}
 * {@code revokedAt} means it has not been revoked.
 */
public record RefreshToken(UUID id, UUID userId, String tokenHash, Instant createdAt, Instant expiresAt,
		Instant revokedAt) {

	public static RefreshToken issue(UUID userId, String tokenHash, Instant now, Duration timeToLive) {
		return new RefreshToken(null, userId, tokenHash, now, now.plus(timeToLive), null);
	}

	public boolean isActiveAt(Instant instant) {
		return revokedAt == null && instant.isBefore(expiresAt);
	}

}
