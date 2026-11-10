package com.micita.micita_project.core.service.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A service of the business")
public record ServiceResponse(
		@Schema(example = "5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f") UUID id,
		@Schema(example = "Corte de cabello") String name,
		@Schema(example = "Corte clásico con lavado", nullable = true) String description,
		@Schema(example = "45", nullable = true) Integer durationMinutes,
		@Schema(description = "Price in COP", example = "35000.00", nullable = true) BigDecimal price,
		@Schema(example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) String status,
		@Schema(description = "Employees that provide the service") List<UUID> employeeIds,
		@Schema(example = "2026-10-29T15:00:00Z") Instant createdAt) {

}
