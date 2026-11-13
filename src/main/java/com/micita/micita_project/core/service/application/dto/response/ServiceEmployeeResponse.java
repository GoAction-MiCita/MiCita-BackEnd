package com.micita.micita_project.core.service.application.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An employee that provides a service")
public record ServiceEmployeeResponse(
		@Schema(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") UUID id,
		@Schema(example = "Ana") String firstName,
		@Schema(example = "Gómez") String lastName,
		@Schema(description = "Always null until employees can have a photo", nullable = true) String photoUrl) {

}
