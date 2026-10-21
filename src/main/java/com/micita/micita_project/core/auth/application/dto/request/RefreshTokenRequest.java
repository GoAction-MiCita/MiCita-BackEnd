package com.micita.micita_project.core.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token issued at login or on the last refresh")
public record RefreshTokenRequest(
		@Schema(example = "q3Zt9lJm0bXc2dE8fGh1iJk4LmN6oPqR7sTuVwXyZ0A") @NotBlank String refreshToken) {

}
