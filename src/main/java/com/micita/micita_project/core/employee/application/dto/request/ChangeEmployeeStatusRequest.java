package com.micita.micita_project.core.employee.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Only {@code ACTIVE} and {@code INACTIVE} are accepted: deleting an employee is a separate operation
 * (HU-21).
 */
@Schema(description = "New status of an employee")
public record ChangeEmployeeStatusRequest(
		@Schema(example = "INACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) @NotNull
		@Pattern(regexp = CreateEmployeeRequest.STATUS_PATTERN, message = "must be ACTIVE or INACTIVE") String status) {

}
