package com.micita.micita_project.core.service.application.dto.request;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optional filters of {@code GET /api/services} (HU-13). {@code null} or empty values don't filter.
 */
@Schema(description = "Optional filters of the service listing, combined with AND")
public record ServiceListFilter(
		@Schema(description = "Text contained in the name, ignoring case (accents are not normalized)",
				example = "cor", maxLength = 100) String name,
		@Schema(description = "ACTIVE or INACTIVE, in uppercase", example = "ACTIVE") String status,
		@Schema(description = "Services that have at least one of these employees assigned") List<UUID> employeeIds) {

	public static ServiceListFilter none() {
		return new ServiceListFilter(null, null, null);
	}

}
