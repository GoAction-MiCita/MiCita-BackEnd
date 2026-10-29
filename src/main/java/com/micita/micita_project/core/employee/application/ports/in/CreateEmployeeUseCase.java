package com.micita.micita_project.core.employee.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.employee.application.dto.request.CreateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;

public interface CreateEmployeeUseCase {

	/**
	 * Creates an employee in the business owned by {@code ownerId}.
	 */
	EmployeeResponse create(UUID ownerId, CreateEmployeeRequest request);

}
