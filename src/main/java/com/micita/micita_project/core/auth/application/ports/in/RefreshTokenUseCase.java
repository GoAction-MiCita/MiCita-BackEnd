package com.micita.micita_project.core.auth.application.ports.in;

import com.micita.micita_project.core.auth.application.dto.request.RefreshTokenRequest;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;

public interface RefreshTokenUseCase {

	/**
	 * Rotates the refresh token: the one presented is revoked and a new access/refresh pair is
	 * issued.
	 * @throws com.micita.micita_project.core.auth.domain.exceptions.InvalidRefreshTokenException
	 * if the token is unknown, expired or revoked
	 */
	TokenResponse refresh(RefreshTokenRequest request);

}
