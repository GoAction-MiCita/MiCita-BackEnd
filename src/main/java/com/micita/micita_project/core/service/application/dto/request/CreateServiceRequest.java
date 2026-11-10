package com.micita.micita_project.core.service.application.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Data for a new service of the caller's business")
public record CreateServiceRequest(
		@Schema(example = "Corte de cabello") @NotBlank @Size(max = 150) String name,
		@Schema(example = "Corte clásico con lavado", nullable = true) @Size(max = 255) String description,
		@Schema(example = "45", nullable = true) @Positive Integer durationMinutes,
		@Schema(description = "Price in COP", example = "35000.00", nullable = true)
		@PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal price,
		@Schema(example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) @NotNull
		@Pattern(regexp = STATUS_PATTERN, message = "must be ACTIVE or INACTIVE") String status,
		@ArraySchema(arraySchema = @Schema(description = "Employees that provide the service", nullable = true),
				schema = @Schema(example = "9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"))
		List<@NotNull UUID> employeeIds) {

	public static final String STATUS_PATTERN = "^(ACTIVE|INACTIVE)$";

}
