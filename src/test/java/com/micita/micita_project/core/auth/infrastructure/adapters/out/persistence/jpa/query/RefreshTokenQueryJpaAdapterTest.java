package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
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
class RefreshTokenQueryJpaAdapterTest {

	private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

	@Mock
	private RefreshTokenJpaRepository refreshTokenJpaRepository;

	@Spy
	private RefreshTokenBDMapper refreshTokenBDMapper = Mappers.getMapper(RefreshTokenBDMapper.class);

	@InjectMocks
	private RefreshTokenQueryJpaAdapter adapter;

	@Test
	void findByTokenHashMapsEntityToDomain() {
		UUID id = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		RefreshTokenEntity entity = new RefreshTokenEntity();
		entity.setId(id);
		entity.setUserId(userId);
		entity.setTokenHash("hash");
		entity.setCreatedAt(NOW);
		entity.setExpiresAt(NOW.plus(Duration.ofDays(7)));
		entity.setRevokedAt(NOW);
		given(refreshTokenJpaRepository.findByTokenHash("hash")).willReturn(Optional.of(entity));

		assertThat(adapter.findByTokenHash("hash"))
			.contains(new RefreshToken(id, userId, "hash", NOW, NOW.plus(Duration.ofDays(7)), NOW));
	}

	@Test
	void findByTokenHashReturnsEmptyWhenMissing() {
		given(refreshTokenJpaRepository.findByTokenHash("unknown")).willReturn(Optional.empty());

		assertThat(adapter.findByTokenHash("unknown")).isEmpty();
	}

}
