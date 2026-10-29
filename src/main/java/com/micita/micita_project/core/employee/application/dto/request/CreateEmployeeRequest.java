package com.micita.micita_project.core.employee.application.dto.request;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Data for a new employee of the caller's business")
public record CreateEmployeeRequest(
		@Schema(example = "Carlos") @NotBlank @Size(max = 100) String firstName,
		@Schema(example = "Gómez") @NotBlank @Size(max = 100) String lastName,
		@Schema(example = "+573001234567", nullable = true)
		@Pattern(regexp = PHONE_PATTERN, message = "must have 7 to 15 digits, optionally starting with +") String phone,
		@Schema(example = "carlos@elcorte.com", nullable = true) @Email @Size(max = 254) String email,
		@Schema(example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) @NotNull
		@Pattern(regexp = STATUS_PATTERN, message = "must be ACTIVE or INACTIVE") String status,
		@ArraySchema(arraySchema = @Schema(description = "Services the employee provides", nullable = true),
				schema = @Schema(example = "5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f"))
		List<@NotNull UUID> serviceIds) {

	/** Same rule as HU-01; an empty value means no phone. */
	public static final String PHONE_PATTERN = "^(\\+?\\d{7,15})?$";

	public static final String STATUS_PATTERN = "^(ACTIVE|INACTIVE)$";

}
