package com.micita.micita_project.core.auth.application.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.core.auth.application.dto.request.RefreshTokenRequest;
import com.micita.micita_project.core.auth.application.ports.out.RefreshTokenGeneratorPort;
import com.micita.micita_project.core.auth.application.ports.out.command.RefreshTokenCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.RefreshTokenQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.models.RefreshToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutCommandHandlerTest {

	private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

	@Mock
	private RefreshTokenQueryRepositoryPort refreshTokenQueryRepository;

	@Mock
	private RefreshTokenCommandRepositoryPort refreshTokenCommandRepository;

	@Mock
	private RefreshTokenGeneratorPort refreshTokenGenerator;

	@Mock
	private Clock clock;

	@InjectMocks
	private LogoutCommandHandler handler;

	@Test
	void logoutRevokesTheRefreshToken() {
		UUID tokenId = UUID.randomUUID();
		given(refreshTokenGenerator.hash("raw-refresh")).willReturn("hashed-refresh");
		given(clock.instant()).willReturn(NOW);
		given(refreshTokenQueryRepository.findByTokenHash("hashed-refresh")).willReturn(Optional.of(
				new RefreshToken(tokenId, UUID.randomUUID(), "hashed-refresh", NOW, NOW.plus(Duration.ofDays(7)), null)));

		handler.logout(new RefreshTokenRequest("raw-refresh"));

		verify(refreshTokenCommandRepository).revoke(tokenId, NOW);
	}

	@Test
	void logoutWithUnknownTokenDoesNothing() {
		given(refreshTokenGenerator.hash("unknown")).willReturn("hashed-unknown");
		given(refreshTokenQueryRepository.findByTokenHash("hashed-unknown")).willReturn(Optional.empty());

		handler.logout(new RefreshTokenRequest("unknown"));

		verify(refreshTokenCommandRepository, never()).revoke(any(), any());
	}

}
