package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.query;

import java.util.Optional;

import com.micita.micita_project.core.auth.application.ports.out.query.RefreshTokenQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.models.RefreshToken;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.mappers.RefreshTokenBDMapper;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.RefreshTokenJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenQueryJpaAdapter implements RefreshTokenQueryRepositoryPort {

	private final RefreshTokenJpaRepository refreshTokenJpaRepository;

	private final RefreshTokenBDMapper refreshTokenBDMapper;

	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return refreshTokenJpaRepository.findByTokenHash(tokenHash).map(refreshTokenBDMapper::toDomain);
	}

}
