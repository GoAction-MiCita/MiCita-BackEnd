package com.micita.micita_project.core.auth.application.ports.in;

import com.micita.micita_project.core.auth.application.dto.request.LoginRequest;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;

public interface LoginUseCase {

	/**
	 * @throws com.micita.micita_project.core.auth.domain.exceptions.InvalidCredentialsException
	 * if the credentials are invalid
	 */
	TokenResponse login(LoginRequest request);

}
