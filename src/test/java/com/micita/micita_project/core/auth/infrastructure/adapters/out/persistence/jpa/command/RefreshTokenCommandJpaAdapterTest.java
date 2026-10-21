package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.auth.domain.models.RefreshToken;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.entities.RefreshTokenEntity;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.mappers.RefreshTokenBDMapper;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.RefreshTokenJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCommandJpaAdapterTest {

	private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

	@Mock
	private RefreshTokenJpaRepository refreshTokenJpaRepository;

	@Spy
	private RefreshTokenBDMapper refreshTokenBDMapper = Mappers.getMapper(RefreshTokenBDMapper.class);

	@InjectMocks
	private RefreshTokenCommandJpaAdapter adapter;

	@Test
	void saveMapsToEntityAndReturnsStoredToken() {
		UUID id = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		given(refreshTokenJpaRepository.save(any())).willAnswer(invocation -> {
			RefreshTokenEntity entity = invocation.getArgument(0);
			entity.setId(id);
			return entity;
		});

		RefreshToken saved = adapter.save(RefreshToken.issue(userId, "hash", NOW, Duration.ofDays(7)));

		assertThat(saved).isEqualTo(new RefreshToken(id, userId, "hash", NOW, NOW.plus(Duration.ofDays(7)), null));
	}

	@Test
	void revokeReportsWhetherARowWasUpdated() {
		UUID active = UUID.randomUUID();
		UUID alreadyRevoked = UUID.randomUUID();
		given(refreshTokenJpaRepository.revokeIfNotRevoked(active, NOW)).willReturn(1);
		given(refreshTokenJpaRepository.revokeIfNotRevoked(alreadyRevoked, NOW)).willReturn(0);

		assertThat(adapter.revoke(active, NOW)).isTrue();
		assertThat(adapter.revoke(alreadyRevoked, NOW)).isFalse();
	}

}
