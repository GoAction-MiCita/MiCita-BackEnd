package com.micita.micita_project.core.employee.application.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An employee of the business with the services they provide")
public record EmployeeDetailResponse(
		@Schema(example = "9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d") UUID id,
		@Schema(example = "Carlos") String firstName,
		@Schema(example = "Gómez") String lastName,
		@Schema(example = "+573001234567", nullable = true) String phone,
		@Schema(example = "carlos@elcorte.com", nullable = true) String email,
		@Schema(example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) String status,
		@Schema(description = "Non-deleted services the employee provides, ordered by name") List<EmployeeServiceRef> services,
		@Schema(example = "2026-10-29T15:00:00Z") Instant createdAt,
		@Schema(example = "2026-11-02T09:30:00Z") Instant updatedAt) {

}
