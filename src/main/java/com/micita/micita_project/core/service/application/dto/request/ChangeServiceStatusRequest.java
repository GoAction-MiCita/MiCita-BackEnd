package com.micita.micita_project.core.service.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Only {@code ACTIVE} and {@code INACTIVE} are accepted: deleting a service is a separate operation
 * (HU-14).
 */
@Schema(description = "New status of a service")
public record ChangeServiceStatusRequest(
		@Schema(example = "INACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) @NotNull
		@Pattern(regexp = CreateServiceRequest.STATUS_PATTERN, message = "must be ACTIVE or INACTIVE") String status) {

}
