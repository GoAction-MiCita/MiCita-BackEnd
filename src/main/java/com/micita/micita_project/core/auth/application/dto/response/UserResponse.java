package com.micita.micita_project.core.auth.application.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Registered user, without credentials")
public record UserResponse(
		@Schema(example = "3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c") UUID id,
		@Schema(example = "Ana") String firstName,
		@Schema(example = "Pérez") String lastName,
		@Schema(example = "Calle 10 # 20-30, Bogotá") String address,
		@Schema(example = "+573001234567") String phone,
		@Schema(example = "ana@example.com") String email,
		@Schema(example = "ana.perez") String username) {

}
