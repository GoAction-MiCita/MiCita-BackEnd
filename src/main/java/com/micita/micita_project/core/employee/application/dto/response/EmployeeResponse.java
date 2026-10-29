package com.micita.micita_project.core.employee.application.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An employee of the business")
public record EmployeeResponse(
		@Schema(example = "9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d") UUID id,
		@Schema(example = "Carlos") String firstName,
		@Schema(example = "Gómez") String lastName,
		@Schema(example = "+573001234567", nullable = true) String phone,
		@Schema(example = "carlos@elcorte.com", nullable = true) String email,
		@Schema(example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) String status,
		@Schema(description = "Services the employee provides") List<UUID> serviceIds,
		@Schema(example = "2026-10-29T15:00:00Z") Instant createdAt) {

}
