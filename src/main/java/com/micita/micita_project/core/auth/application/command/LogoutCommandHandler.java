package com.micita.micita_project.core.auth.application.command;

import java.time.Clock;
import java.time.Instant;

import com.micita.micita_project.core.auth.application.dto.request.RefreshTokenRequest;
import com.micita.micita_project.core.auth.application.ports.in.LogoutUseCase;
import com.micita.micita_project.core.auth.application.ports.out.RefreshTokenGeneratorPort;
import com.micita.micita_project.core.auth.application.ports.out.command.RefreshTokenCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.RefreshTokenQueryRepositoryPort;

import org.springframework.stereotype.Service;

@Service
public class LogoutCommandHandler implements LogoutUseCase {

	private final RefreshTokenQueryRepositoryPort refreshTokenQueryRepository;

	private final RefreshTokenCommandRepositoryPort refreshTokenCommandRepository;

	private final RefreshTokenGeneratorPort refreshTokenGenerator;

	private final Clock clock;

	public LogoutCommandHandler(RefreshTokenQueryRepositoryPort refreshTokenQueryRepository,
			RefreshTokenCommandRepositoryPort refreshTokenCommandRepository,
			RefreshTokenGeneratorPort refreshTokenGenerator, Clock clock) {
		this.refreshTokenQueryRepository = refreshTokenQueryRepository;
		this.refreshTokenCommandRepository = refreshTokenCommandRepository;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.clock = clock;
	}

	@Override
	public void logout(RefreshTokenRequest request) {
		refreshTokenQueryRepository.findByTokenHash(refreshTokenGenerator.hash(request.refreshToken()))
			.ifPresent(token -> refreshTokenCommandRepository.revoke(token.id(), Instant.now(clock)));
	}

}
