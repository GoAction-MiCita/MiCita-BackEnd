package com.micita.micita_project.core.auth.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionTokensIssuerTest {

	private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

	private static final AuthenticatedUser USER = new AuthenticatedUser(UUID.randomUUID(), "ana", List.of());

	@Mock
	private TokenIssuerPort tokenIssuer;

	@Mock
	private RefreshTokenGeneratorPort refreshTokenGenerator;

	@Mock
	private RefreshTokenCommandRepositoryPort refreshTokenCommandRepository;

	@Mock
	private BusinessQueryPort businessQuery;

	private SessionTokensIssuer sessionTokensIssuer;

	@BeforeEach
	void setUp() {
		// JwtProperties and Clock are values, so the real ones are used instead of mocks.
		JwtProperties properties = new JwtProperties("unit-test-secret-0123456789abcdef0123456789", "micita",
				Duration.ofMinutes(15), Duration.ofDays(7));
		sessionTokensIssuer = new SessionTokensIssuer(tokenIssuer, refreshTokenGenerator,
				refreshTokenCommandRepository, businessQuery, Mappers.getMapper(TokenMapper.class), properties,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void issueReturnsAccessAndRefreshTokensAndStoresOnlyTheRefreshHash() {
		given(businessQuery.existsForOwner(USER.userId())).willReturn(false);
		given(tokenIssuer.issue(USER, false)).willReturn(new IssuedToken("jwt-token", 900));
		given(refreshTokenGenerator.generate()).willReturn("raw-refresh");
		given(refreshTokenGenerator.hash("raw-refresh")).willReturn("hashed-refresh");

		TokenResponse response = sessionTokensIssuer.issue(USER);

		assertThat(response).isEqualTo(new TokenResponse("jwt-token", "Bearer", 900, "raw-refresh", 604800));
		ArgumentCaptor<RefreshToken> stored = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenCommandRepository).save(stored.capture());
		assertThat(stored.getValue()).isEqualTo(new RefreshToken(null, USER.userId(), "hashed-refresh", NOW,
				NOW.plus(Duration.ofDays(7)), null));
	}

	@Test
	void issuePassesHasBusinessFlagToTokenIssuer() {
		given(businessQuery.existsForOwner(USER.userId())).willReturn(true);
		given(tokenIssuer.issue(any(), anyBoolean())).willReturn(new IssuedToken("jwt-token", 900));
		given(refreshTokenGenerator.generate()).willReturn("raw-refresh");

		sessionTokensIssuer.issue(USER);

		verify(tokenIssuer).issue(USER, true);
	}

}
