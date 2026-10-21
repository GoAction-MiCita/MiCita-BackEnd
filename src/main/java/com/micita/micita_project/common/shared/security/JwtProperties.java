package com.micita.micita_project.common.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param expiration lifetime of access tokens (JWT)
 * @param refreshExpiration lifetime of refresh tokens
 */
@ConfigurationProperties("micita.security.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration, Duration refreshExpiration) {

	private static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"micita.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
		}
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("micita.security.jwt.issuer must not be blank");
		}
		requirePositive(expiration, "micita.security.jwt.expiration");
		requirePositive(refreshExpiration, "micita.security.jwt.refresh-expiration");
	}

	private static void requirePositive(Duration duration, String property) {
		if (duration == null || duration.isNegative() || duration.isZero()) {
			throw new IllegalArgumentException(property + " must be positive");
		}
	}

}
