package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.command;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.auth.application.ports.out.command.RefreshTokenCommandRepositoryPort;
import com.micita.micita_project.core.auth.domain.models.RefreshToken;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.mappers.RefreshTokenBDMapper;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.RefreshTokenJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenCommandJpaAdapter implements RefreshTokenCommandRepositoryPort {

	private final RefreshTokenJpaRepository refreshTokenJpaRepository;

	private final RefreshTokenBDMapper refreshTokenBDMapper;

	public RefreshTokenCommandJpaAdapter(RefreshTokenJpaRepository refreshTokenJpaRepository,
			RefreshTokenBDMapper refreshTokenBDMapper) {
		this.refreshTokenJpaRepository = refreshTokenJpaRepository;
		this.refreshTokenBDMapper = refreshTokenBDMapper;
	}

	@Override
	public RefreshToken save(RefreshToken refreshToken) {
		return refreshTokenBDMapper
			.toDomain(refreshTokenJpaRepository.save(refreshTokenBDMapper.toEntity(refreshToken)));
	}

	@Override
	@Transactional
	public boolean revoke(UUID id, Instant revokedAt) {
		return refreshTokenJpaRepository.revokeIfNotRevoked(id, revokedAt) > 0;
	}

}
