package com.micita.micita_project.core.employee.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.employee.application.dto.response.EmployeeDetailResponse;

public interface GetEmployeeUseCase {

	/**
	 * @return the employee of the business owned by {@code ownerId}, with its non-deleted services
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the employee
	 * doesn't exist, belongs to another business or is deleted
	 */
	EmployeeDetailResponse get(UUID ownerId, UUID employeeId);

}
