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

/**
 * Same rules as {@link CreateEmployeeRequest} (HU-17). {@code serviceIds} replaces the whole assignment
 * when sent: an empty list removes every service, and leaving it out keeps the current ones.
 */
@Schema(description = "New data of an employee of the caller's business")
public record UpdateEmployeeRequest(
		@Schema(example = "Carlos") @NotBlank @Size(max = 100) String firstName,
		@Schema(example = "Gómez") @NotBlank @Size(max = 100) String lastName,
		@Schema(example = "+573001234567", nullable = true) @Pattern(regexp = CreateEmployeeRequest.PHONE_PATTERN,
				message = "must have 7 to 15 digits, optionally starting with +") String phone,
		@Schema(example = "carlos@elcorte.com", nullable = true) @Email @Size(max = 254) String email,
		@Schema(example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) @NotNull
		@Pattern(regexp = CreateEmployeeRequest.STATUS_PATTERN, message = "must be ACTIVE or INACTIVE") String status,
		@ArraySchema(
				arraySchema = @Schema(description = "Services the employee provides. Replaces the current ones; an "
						+ "empty list removes them all and leaving it out keeps them", nullable = true),
				schema = @Schema(example = "5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f"))
		List<@NotNull UUID> serviceIds) {

}
