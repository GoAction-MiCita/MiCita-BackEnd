package com.micita.micita_project.core.auth.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.auth.application.dto.request.RefreshTokenRequest;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.application.ports.out.RefreshTokenGeneratorPort;
import com.micita.micita_project.core.auth.application.ports.out.command.RefreshTokenCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.RefreshTokenQueryRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.exceptions.InvalidRefreshTokenException;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.RefreshToken;
import com.micita.micita_project.core.auth.domain.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenCommandHandlerTest {

	private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

	private static final UUID TOKEN_ID = UUID.randomUUID();

	private static final UUID USER_ID = UUID.randomUUID();

	private static final RefreshTokenRequest REQUEST = new RefreshTokenRequest("raw-refresh");

	@Mock
	private RefreshTokenQueryRepositoryPort refreshTokenQueryRepository;

	@Mock
	private RefreshTokenCommandRepositoryPort refreshTokenCommandRepository;

	@Mock
	private RefreshTokenGeneratorPort refreshTokenGenerator;

	@Mock
	private UserQueryRepositoryPort userQueryRepository;

	@Mock
	private SessionTokensIssuer sessionTokensIssuer;

	@Mock
	private Clock clock;

	@InjectMocks
	private RefreshTokenCommandHandler handler;

	@BeforeEach
	void setUp() {
		given(clock.instant()).willReturn(NOW);
		given(refreshTokenGenerator.hash("raw-refresh")).willReturn("hashed-refresh");
	}

	@Test
	void validRefreshTokenIsRotatedIntoNewTokens() {
		User user = new User(USER_ID, "Ana", "Pérez", "Calle 10", "+573001234567", "ana@example.com", "ana",
				"{bcrypt}hash", Set.of("ADMIN"));
		TokenResponse tokens = new TokenResponse("new-jwt", "Bearer", 900, "new-refresh", 604800);
		given(refreshTokenQueryRepository.findByTokenHash("hashed-refresh"))
			.willReturn(Optional.of(token(NOW.plus(Duration.ofDays(1)), null)));
		given(refreshTokenCommandRepository.revoke(TOKEN_ID, NOW)).willReturn(true);
		given(userQueryRepository.findById(USER_ID)).willReturn(Optional.of(user));
		given(sessionTokensIssuer.issue(AuthenticatedUser.of(user))).willReturn(tokens);

		assertThat(handler.refresh(REQUEST)).isEqualTo(tokens);
		verify(refreshTokenCommandRepository).revoke(TOKEN_ID, NOW);
	}

	@Test
	void unknownRefreshTokenIsRejected() {
		given(refreshTokenQueryRepository.findByTokenHash("hashed-refresh")).willReturn(Optional.empty());

		assertRejectedWithoutNewTokens();
	}

	@Test
	void expiredRefreshTokenIsRejected() {
		given(refreshTokenQueryRepository.findByTokenHash("hashed-refresh"))
			.willReturn(Optional.of(token(NOW, null)));

		assertRejectedWithoutNewTokens();
		verify(refreshTokenCommandRepository, never()).revoke(any(), any());
	}

	@Test
	void revokedRefreshTokenIsRejected() {
		given(refreshTokenQueryRepository.findByTokenHash("hashed-refresh"))
			.willReturn(Optional.of(token(NOW.plus(Duration.ofDays(1)), NOW.minusSeconds(60))));

		assertRejectedWithoutNewTokens();
		verify(refreshTokenCommandRepository, never()).revoke(any(), any());
	}

	@Test
	void refreshTokenUsedConcurrentlyIsRejected() {
		given(refreshTokenQueryRepository.findByTokenHash("hashed-refresh"))
			.willReturn(Optional.of(token(NOW.plus(Duration.ofDays(1)), null)));
		given(refreshTokenCommandRepository.revoke(TOKEN_ID, NOW)).willReturn(false);

		assertRejectedWithoutNewTokens();
	}

	@Test
	void refreshTokenOfDeletedUserIsRejected() {
		given(refreshTokenQueryRepository.findByTokenHash("hashed-refresh"))
			.willReturn(Optional.of(token(NOW.plus(Duration.ofDays(1)), null)));
		given(refreshTokenCommandRepository.revoke(TOKEN_ID, NOW)).willReturn(true);
		given(userQueryRepository.findById(USER_ID)).willReturn(Optional.empty());

		assertRejectedWithoutNewTokens();
	}

	private void assertRejectedWithoutNewTokens() {
		assertThatThrownBy(() -> handler.refresh(REQUEST)).isInstanceOf(InvalidRefreshTokenException.class)
			.hasMessage("Invalid or expired refresh token");
		verify(sessionTokensIssuer, never()).issue(any());
	}

	private static RefreshToken token(Instant expiresAt, Instant revokedAt) {
		return new RefreshToken(TOKEN_ID, USER_ID, "hashed-refresh", NOW.minus(Duration.ofDays(1)), expiresAt,
				revokedAt);
	}

}
