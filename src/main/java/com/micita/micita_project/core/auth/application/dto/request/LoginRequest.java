package com.micita.micita_project.core.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User credentials")
public record LoginRequest(
		@Schema(example = "admin") @NotBlank String username,
		@Schema(example = "admin") @NotBlank String password) {

}
