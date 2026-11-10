package com.micita.micita_project.core.service.infrastructure.adapters.in.rest;

import com.micita.micita_project.common.shared.security.CurrentUserId;
import com.micita.micita_project.core.service.application.dto.request.CreateServiceRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;
import com.micita.micita_project.core.service.application.ports.in.CreateServiceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Services offered by the caller's business")
@RequiredArgsConstructor
public class ServiceController {

	private final CreateServiceUseCase createServiceUseCase;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a service",
			description = "Creates a service in the caller's business and, if employeeIds is sent, assigns those "
					+ "employees. The name must not be used, ignoring case, by another non-deleted service of the "
					+ "business")
	@ApiResponse(responseCode = "201", description = "Service registered")
	@ApiResponse(responseCode = "400",
			description = "Missing or invalid fields (errors map), or employeeIds with unknown, deleted or foreign "
					+ "employees (listed in rejectedIds)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	@ApiResponse(responseCode = "409", description = "Another service of the business uses the name",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public ServiceResponse create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateServiceRequest request) {
		return createServiceUseCase.create(CurrentUserId.from(jwt), request);
	}

}
