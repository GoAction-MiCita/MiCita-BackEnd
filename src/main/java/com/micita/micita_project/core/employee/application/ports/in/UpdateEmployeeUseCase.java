package com.micita.micita_project.core.employee.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.employee.application.dto.request.UpdateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;

public interface UpdateEmployeeUseCase {

	/**
	 * Replaces the data of an employee of the business owned by {@code ownerId} and, when
	 * {@code serviceIds} is sent, its whole service assignment. Nothing is stored if any check fails.
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the employee
	 * doesn't exist, belongs to another business or is deleted
	 * @throws com.micita.micita_project.core.employee.domain.exceptions.DuplicateEmployeeException if
	 * another employee of the business uses the email or phone
	 * @throws com.micita.micita_project.common.shared.exception.InvalidReferencesException if any service
	 * is unknown, of another business or deleted
	 */
	EmployeeResponse update(UUID ownerId, UUID employeeId, UpdateEmployeeRequest request);

}
