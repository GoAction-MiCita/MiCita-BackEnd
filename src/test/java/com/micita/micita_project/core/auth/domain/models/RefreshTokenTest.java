package com.micita.micita_project.core.auth.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RefreshTokenTest {

	private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

	@Test
	void issuedTokenIsActiveUntilItExpires() {
		RefreshToken token = RefreshToken.issue(UUID.randomUUID(), "hash", NOW, Duration.ofDays(7));

		assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
		assertThat(token.isActiveAt(NOW)).isTrue();
		assertThat(token.isActiveAt(token.expiresAt().minusMillis(1))).isTrue();
		assertThat(token.isActiveAt(token.expiresAt())).isFalse();
	}

	@Test
	void revokedTokenIsNotActive() {
		RefreshToken token = new RefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW,
				NOW.plus(Duration.ofDays(7)), NOW);

		assertThat(token.isActiveAt(NOW)).isFalse();
	}

}
