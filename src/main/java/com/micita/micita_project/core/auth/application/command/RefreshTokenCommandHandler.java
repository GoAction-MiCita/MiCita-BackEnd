package com.micita.micita_project.core.auth.application.command;

import java.time.Clock;
import java.time.Instant;

import com.micita.micita_project.core.auth.application.dto.request.RefreshTokenRequest;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.application.ports.in.RefreshTokenUseCase;
import com.micita.micita_project.core.auth.application.ports.out.RefreshTokenGeneratorPort;
import com.micita.micita_project.core.auth.application.ports.out.command.RefreshTokenCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.RefreshTokenQueryRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.exceptions.InvalidRefreshTokenException;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.RefreshToken;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenCommandHandler implements RefreshTokenUseCase {

	private final RefreshTokenQueryRepositoryPort refreshTokenQueryRepository;

	private final RefreshTokenCommandRepositoryPort refreshTokenCommandRepository;

	private final RefreshTokenGeneratorPort refreshTokenGenerator;

	private final UserQueryRepositoryPort userQueryRepository;

	private final SessionTokensIssuer sessionTokensIssuer;

	private final Clock clock;

	public RefreshTokenCommandHandler(RefreshTokenQueryRepositoryPort refreshTokenQueryRepository,
			RefreshTokenCommandRepositoryPort refreshTokenCommandRepository,
			RefreshTokenGeneratorPort refreshTokenGenerator, UserQueryRepositoryPort userQueryRepository,
			SessionTokensIssuer sessionTokensIssuer, Clock clock) {
		this.refreshTokenQueryRepository = refreshTokenQueryRepository;
		this.refreshTokenCommandRepository = refreshTokenCommandRepository;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.userQueryRepository = userQueryRepository;
		this.sessionTokensIssuer = sessionTokensIssuer;
		this.clock = clock;
	}

	@Override
	@Transactional
	public TokenResponse refresh(RefreshTokenRequest request) {
		Instant now = Instant.now(clock);
		RefreshToken current = refreshTokenQueryRepository
			.findByTokenHash(refreshTokenGenerator.hash(request.refreshToken()))
			.filter(token -> token.isActiveAt(now))
			.orElseThrow(InvalidRefreshTokenException::new);
		// Conditional revoke: if a concurrent refresh already used this token, only one wins.
		if (!refreshTokenCommandRepository.revoke(current.id(), now)) {
			throw new InvalidRefreshTokenException();
		}
		AuthenticatedUser user = userQueryRepository.findById(current.userId())
			.map(AuthenticatedUser::of)
			.orElseThrow(InvalidRefreshTokenException::new);
		return sessionTokensIssuer.issue(user);
	}

}
