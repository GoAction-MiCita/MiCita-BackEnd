package com.micita.micita_project.core.auth.application.command;

import com.micita.micita_project.core.auth.application.dto.request.LoginRequest;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.application.ports.in.LoginUseCase;
import com.micita.micita_project.core.auth.application.ports.out.CredentialsAuthenticatorPort;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginCommandHandler implements LoginUseCase {

	private final CredentialsAuthenticatorPort credentialsAuthenticator;

	private final SessionTokensIssuer sessionTokensIssuer;

	@Override
	public TokenResponse login(LoginRequest request) {
		AuthenticatedUser user = credentialsAuthenticator.authenticate(request.username(), request.password());
		return sessionTokensIssuer.issue(user);
	}

}
