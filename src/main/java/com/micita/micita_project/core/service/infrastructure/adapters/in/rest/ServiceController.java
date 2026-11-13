package com.micita.micita_project.core.service.infrastructure.adapters.in.rest;

import java.util.List;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.common.shared.security.CurrentUserId;
import com.micita.micita_project.core.service.application.dto.request.CreateServiceRequest;
import com.micita.micita_project.core.service.application.dto.request.ServiceListFilter;
import com.micita.micita_project.core.service.application.dto.response.ServiceListItemResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;
import com.micita.micita_project.core.service.application.ports.in.CreateServiceUseCase;
import com.micita.micita_project.core.service.application.ports.in.ListServicesUseCase;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Services offered by the caller's business")
@RequiredArgsConstructor
public class ServiceController {

	private final CreateServiceUseCase createServiceUseCase;

	private final ListServicesUseCase listServicesUseCase;

	@GetMapping
	@Operation(summary = "List the services of the caller's business",
			description = "Paginated, by name ascending (ignoring case). Deleted services and deleted employees are "
					+ "left out, and services of other businesses never appear. Each service carries the employees "
					+ "that provide it; photoUrl is null until employees can have a photo. The optional filters name, "
					+ "status and employeeIds are combined with AND, and page totals count only the matching services")
	@ApiResponse(responseCode = "200", description = "Page of services; empty when the business has none")
	@ApiResponse(responseCode = "400",
			description = "page is negative, size is outside 1..50, name is longer than 100 characters, status is "
					+ "not ACTIVE or INACTIVE, or employeeIds has a malformed id (errors map); or employeeIds has "
					+ "unknown, deleted or foreign employees (listed in rejectedIds)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	public PageResponse<ServiceListItemResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size, 1..50") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Text contained in the name, ignoring case. Accents are not normalized",
					example = "cor") @RequestParam(required = false) String name,
			@Parameter(description = "ACTIVE or INACTIVE, uppercase only",
					schema = @Schema(allowableValues = { "ACTIVE", "INACTIVE" })) @RequestParam(
							required = false) String status,
			@Parameter(description = "Comma-separated employee ids: services that have at least one of them")
			@RequestParam(required = false) List<UUID> employeeIds) {
		return listServicesUseCase.list(CurrentUserId.from(jwt), new ServiceListFilter(name, status, employeeIds),
				page, size);
	}

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
