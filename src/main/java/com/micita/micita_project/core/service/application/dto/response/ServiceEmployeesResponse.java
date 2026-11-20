package com.micita.micita_project.core.service.application.dto.response;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A service with the employees that now provide it")
public record ServiceEmployeesResponse(
		@Schema(example = "5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f") UUID id,
		@Schema(description = "Assigned employees, without repeats, in the order received") List<UUID> employeeIds) {

}
