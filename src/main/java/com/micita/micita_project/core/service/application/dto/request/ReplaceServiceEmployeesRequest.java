package com.micita.micita_project.core.service.application.dto.request;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * The list is required: an empty one removes every employee, but a missing one is a mistake and must not
 * wipe the assignments.
 */
@Schema(description = "Employees that will provide the service, replacing the current ones")
public record ReplaceServiceEmployeesRequest(
		@ArraySchema(arraySchema = @Schema(description = "Empty removes every employee. Repeated ids count once",
				requiredMode = Schema.RequiredMode.REQUIRED),
				schema = @Schema(example = "9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"))
		@NotNull List<@NotNull UUID> employeeIds) {

}
