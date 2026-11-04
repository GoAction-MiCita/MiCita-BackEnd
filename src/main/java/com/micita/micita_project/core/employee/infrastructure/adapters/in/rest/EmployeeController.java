package com.micita.micita_project.core.employee.infrastructure.adapters.in.rest;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.common.shared.security.CurrentUserId;
import com.micita.micita_project.core.employee.application.dto.request.CreateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeListItemResponse;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
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

	@GetMapping
	@Operation(summary = "List the employees of the caller's business",
			description = "Paginated, newest first. Deleted employees and deleted services are left out, and "
					+ "employees of other businesses never appear")
	@ApiResponse(responseCode = "200", description = "Page of employees")
	@ApiResponse(responseCode = "400",
			description = "page is negative or size is outside 1..50 (errors map)",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	public PageResponse<EmployeeListItemResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		return listEmployeesUseCase.list(CurrentUserId.from(jwt), page, size);
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

}
