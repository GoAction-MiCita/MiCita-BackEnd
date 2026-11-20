package com.micita.micita_project.core.service.infrastructure.adapters.in.rest;

import java.util.List;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.common.shared.security.CurrentUserId;
import com.micita.micita_project.core.service.application.dto.request.ChangeServiceStatusRequest;
import com.micita.micita_project.core.service.application.dto.request.CreateServiceRequest;
import com.micita.micita_project.core.service.application.dto.request.ServiceListFilter;
import com.micita.micita_project.core.service.application.dto.request.ReplaceServiceEmployeesRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceEmployeesResponse;
import com.micita.micita_project.core.service.application.ports.in.ReplaceServiceEmployeesUseCase;
import com.micita.micita_project.core.service.application.dto.response.ServiceListItemResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceStatusResponse;
import com.micita.micita_project.core.service.application.ports.in.ChangeServiceStatusUseCase;
import com.micita.micita_project.core.service.application.ports.in.CreateServiceUseCase;
import com.micita.micita_project.core.service.application.ports.in.DeleteServiceUseCase;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	private final ChangeServiceStatusUseCase changeServiceStatusUseCase;

	private final DeleteServiceUseCase deleteServiceUseCase;

	private final ReplaceServiceEmployeesUseCase replaceServiceEmployeesUseCase;

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

	@PatchMapping("/{id}/status")
	@Operation(summary = "Activate or deactivate a service",
			description = "Sets the status of a service of the caller's business to ACTIVE or INACTIVE. Sending the "
					+ "current status changes nothing. Existing appointments are kept; inactive services can't be "
					+ "booked. Deleting a service is a separate operation")
	@ApiResponse(responseCode = "200", description = "Id and new status of the service")
	@ApiResponse(responseCode = "400",
			description = "status is missing or is not ACTIVE or INACTIVE (DELETED included), or the id is malformed "
					+ "(errors map)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	@ApiResponse(responseCode = "404",
			description = "The service doesn't exist, belongs to another business or is deleted",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public ServiceStatusResponse changeStatus(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Service id") @PathVariable UUID id,
			@Valid @RequestBody ChangeServiceStatusRequest request) {
		return changeServiceStatusUseCase.changeStatus(CurrentUserId.from(jwt), id, request);
	}

	@PutMapping("/{id}/employees")
	@Operation(summary = "Replace the employees of a service",
			description = "The employees that provide the service become exactly employeeIds: an empty list removes "
					+ "all of them and repeated ids count once. ACTIVE and INACTIVE employees can be assigned; "
					+ "unknown, deleted or foreign ones reject the whole request and nothing changes")
	@ApiResponse(responseCode = "200", description = "Id of the service and the employees now assigned")
	@ApiResponse(responseCode = "400",
			description = "employeeIds is missing or has a malformed id, or the service id is malformed (errors map); "
					+ "or employeeIds has unknown, deleted or foreign employees (listed in rejectedIds)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	@ApiResponse(responseCode = "404",
			description = "The service doesn't exist, belongs to another business or is deleted",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public ServiceEmployeesResponse replaceEmployees(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Service id") @PathVariable UUID id,
			@Valid @RequestBody ReplaceServiceEmployeesRequest request) {
		return replaceServiceEmployeesUseCase.replaceEmployees(CurrentUserId.from(jwt), id, request);
	}

	@PutMapping("/{id}/delete")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a service",
			description = "Soft delete: the service becomes DELETED and is kept, together with its employee "
					+ "assignments, because past appointments reference it. It then drops out of every listing, "
					+ "can't be booked, and its name can be reused. Services with future appointments can't be "
					+ "deleted; deactivate them instead")
	@ApiResponse(responseCode = "204", description = "Service deleted")
	@ApiResponse(responseCode = "400", description = "The id is malformed (errors map)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	@ApiResponse(responseCode = "404",
			description = "The service doesn't exist, belongs to another business or is already deleted",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409",
			description = "The service has future appointments (count in futureAppointments)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public void delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Service id") @PathVariable UUID id) {
		deleteServiceUseCase.delete(CurrentUserId.from(jwt), id);
	}

}
