package com.micita.micita_project.core.employee.application.dto.request;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optional filters of {@code GET /api/employees} (HU-22). {@code null} or empty values don't filter.
 */
@Schema(description = "Optional filters of the employee listing, combined with AND")
public record EmployeeListFilter(
		@Schema(description = "Text contained in the first or last name, ignoring case (accents are not "
				+ "normalized)", example = "ana", maxLength = 100) String name,
		@Schema(description = "ACTIVE or INACTIVE, in uppercase", example = "ACTIVE") String status,
		@Schema(description = "Employees that provide at least one of these services") List<UUID> serviceIds) {

	public static EmployeeListFilter none() {
		return new EmployeeListFilter(null, null, null);
	}

}
