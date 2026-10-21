package com.micita.micita_project.core.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issued access and refresh tokens")
public record TokenResponse(
		@Schema(description = "Signed JWT. Carries the roles and hasBusiness claims") String accessToken,
		@Schema(example = "Bearer") String tokenType,
		@Schema(description = "Seconds until the access token expires", example = "900") long expiresIn,
		@Schema(description = "Opaque token for POST /api/auth/refresh. It is single-use: each refresh returns a new one")
		String refreshToken,
		@Schema(description = "Seconds until the refresh token expires", example = "604800") long refreshExpiresIn) {

}
