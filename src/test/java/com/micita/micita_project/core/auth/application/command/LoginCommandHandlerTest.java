package com.micita.micita_project.core.auth.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import com.micita.micita_project.core.auth.application.dto.request.LoginRequest;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.application.ports.out.CredentialsAuthenticatorPort;
import com.micita.micita_project.core.auth.domain.exceptions.InvalidCredentialsException;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

	@Mock
	private CredentialsAuthenticatorPort credentialsAuthenticator;

	@Mock
	private SessionTokensIssuer sessionTokensIssuer;

	@InjectMocks
	private LoginCommandHandler loginCommandHandler;

	@Test
	void loginStartsSessionForAuthenticatedUser() {
		AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "ana", List.of("ADMIN"));
		TokenResponse tokens = new TokenResponse("jwt-token", "Bearer", 900, "refresh-token", 604800);
		given(credentialsAuthenticator.authenticate("ana", "secret")).willReturn(user);
		given(sessionTokensIssuer.issue(user)).willReturn(tokens);

		assertThat(loginCommandHandler.login(new LoginRequest("ana", "secret"))).isEqualTo(tokens);
	}

	@Test
	void loginPropagatesInvalidCredentialsWithoutIssuingTokens() {
		given(credentialsAuthenticator.authenticate("ana", "wrong")).willThrow(new InvalidCredentialsException());

		assertThatThrownBy(() -> loginCommandHandler.login(new LoginRequest("ana", "wrong")))
			.isInstanceOf(InvalidCredentialsException.class);
		verify(sessionTokensIssuer, never()).issue(any());
	}

}
