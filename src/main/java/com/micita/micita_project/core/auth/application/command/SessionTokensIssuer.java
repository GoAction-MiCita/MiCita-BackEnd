package com.micita.micita_project.core.auth.application.command;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.micita.micita_project.common.shared.security.JwtProperties;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.application.mappers.TokenMapper;
import com.micita.micita_project.core.auth.application.ports.out.RefreshTokenGeneratorPort;
import com.micita.micita_project.core.auth.application.ports.out.TokenIssuerPort;
import com.micita.micita_project.core.auth.application.ports.out.command.RefreshTokenCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.IssuedToken;
import com.micita.micita_project.core.auth.domain.models.RefreshToken;
import com.micita.micita_project.core.auth.domain.models.SessionTokens;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Starts a session for an authenticated user: an access token with the {@code hasBusiness} flag
 * plus a new refresh token, of which only the hash is stored. Shared by login and refresh.
 */
@Component
@RequiredArgsConstructor
public class SessionTokensIssuer {

	private final TokenIssuerPort tokenIssuer;

	private final RefreshTokenGeneratorPort refreshTokenGenerator;

	private final RefreshTokenCommandRepositoryPort refreshTokenCommandRepository;

	private final BusinessQueryPort businessQuery;

	private final TokenMapper tokenMapper;

	private final JwtProperties jwtProperties;

	private final Clock clock;

	public TokenResponse issue(AuthenticatedUser user) {
		IssuedToken accessToken = tokenIssuer.issue(user, businessQuery.existsForOwner(user.userId()));
		String refreshToken = refreshTokenGenerator.generate();
		Duration refreshTimeToLive = jwtProperties.refreshExpiration();
		refreshTokenCommandRepository.save(RefreshToken.issue(user.userId(), refreshTokenGenerator.hash(refreshToken),
				Instant.now(clock), refreshTimeToLive));
		return tokenMapper.toResponse(new SessionTokens(accessToken, refreshToken, refreshTimeToLive.toSeconds()));
	}

}
