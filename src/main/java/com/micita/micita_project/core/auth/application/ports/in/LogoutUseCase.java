package com.micita.micita_project.core.auth.application.ports.in;

import com.micita.micita_project.core.auth.application.dto.request.RefreshTokenRequest;

public interface LogoutUseCase {

	/**
	 * Revokes the refresh token. Unknown or already revoked tokens are ignored, so the call is
	 * idempotent.
	 */
	void logout(RefreshTokenRequest request);

}
