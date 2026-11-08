package com.micita.micita_project.core.employee.infrastructure.adapters.in.rest;

import java.util.List;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.common.shared.security.CurrentUserId;
import com.micita.micita_project.core.employee.application.dto.request.ChangeEmployeeStatusRequest;
import com.micita.micita_project.core.employee.application.dto.request.CreateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.request.EmployeeListFilter;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeListItemResponse;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.ports.in.ChangeEmployeeStatusUseCase;
import com.micita.micita_project.core.employee.application.ports.in.CreateEmployeeUseCase;
import com.micita.micita_project.core.employee.application.ports.in.ListEmployeesUseCase;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employees of the caller's business")
@RequiredArgsConstructor
public class EmployeeController {

	private final CreateEmployeeUseCase createEmployeeUseCase;

	private final ListEmployeesUseCase listEmployeesUseCase;

	private final ChangeEmployeeStatusUseCase changeEmployeeStatusUseCase;

	@GetMapping
	@Operation(summary = "List the employees of the caller's business",
			description = "Paginated, newest first. Deleted employees and deleted services are left out, and "
					+ "employees of other businesses never appear. The optional filters name, status and "
					+ "serviceIds are combined with AND, and page totals count only the matching employees")
	@ApiResponse(responseCode = "200", description = "Page of employees")
	@ApiResponse(responseCode = "400",
			description = "page is negative, size is outside 1..50, name is longer than 100 characters, status "
					+ "is not ACTIVE or INACTIVE, or serviceIds has a malformed id (errors map); or serviceIds "
					+ "has unknown, deleted or foreign services (listed in rejectedIds)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	public PageResponse<EmployeeListItemResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size, 1..50") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Text contained in the first or last name, ignoring case. Accents are not "
					+ "normalized", example = "ana") @RequestParam(required = false) String name,
			@Parameter(description = "ACTIVE or INACTIVE, uppercase only",
					schema = @Schema(allowableValues = { "ACTIVE", "INACTIVE" })) @RequestParam(
							required = false) String status,
			@Parameter(description = "Comma-separated service ids: employees that provide at least one of them")
			@RequestParam(required = false) List<UUID> serviceIds) {
		return listEmployeesUseCase.list(CurrentUserId.from(jwt), new EmployeeListFilter(name, status, serviceIds),
				page, size);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register an employee",
			description = "Creates an employee in the caller's business and, if serviceIds is sent, assigns those "
					+ "services. Email and phone must not be used by another employee of the business")
	@ApiResponse(responseCode = "201", description = "Employee registered")
	@ApiResponse(responseCode = "400",
			description = "Missing or invalid fields, or serviceIds with unknown, deleted or foreign services "
					+ "(listed in rejectedIds)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	@ApiResponse(responseCode = "409", description = "Another employee of the business uses the email or phone",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public EmployeeResponse create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateEmployeeRequest request) {
		return createEmployeeUseCase.create(CurrentUserId.from(jwt), request);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Activate or deactivate an employee",
			description = "Sets the status of an employee of the caller's business to ACTIVE or INACTIVE. Sending "
					+ "the current status changes nothing. Service assignments and existing appointments are kept; "
					+ "inactive employees can't take new appointments. Deleting an employee is a separate operation")
	@ApiResponse(responseCode = "200", description = "Employee with its new status")
	@ApiResponse(responseCode = "400",
			description = "status is missing or is not ACTIVE or INACTIVE (DELETED included), or the id is malformed "
					+ "(errors map)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	@ApiResponse(responseCode = "404",
			description = "The employee doesn't exist, belongs to another business or is deleted",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public EmployeeResponse changeStatus(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Parameter(description = "Employee id") @PathVariable UUID id,
			@Valid @RequestBody ChangeEmployeeStatusRequest request) {
		return changeEmployeeStatusUseCase.changeStatus(CurrentUserId.from(jwt), id, request);
	}

}
