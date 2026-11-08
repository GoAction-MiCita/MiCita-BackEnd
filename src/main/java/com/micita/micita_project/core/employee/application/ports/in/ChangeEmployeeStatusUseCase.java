package com.micita.micita_project.core.employee.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.employee.application.dto.request.ChangeEmployeeStatusRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;

public interface ChangeEmployeeStatusUseCase {

	/**
	 * Activates or deactivates an employee of the business owned by {@code ownerId}. Asking for the
	 * current status changes nothing. Service assignments are kept either way.
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the employee
	 * doesn't exist, belongs to another business or is deleted
	 */
	EmployeeResponse changeStatus(UUID ownerId, UUID employeeId, ChangeEmployeeStatusRequest request);

}
