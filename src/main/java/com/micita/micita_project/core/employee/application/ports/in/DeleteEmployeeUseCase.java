package com.micita.micita_project.core.employee.application.ports.in;

import java.util.UUID;

public interface DeleteEmployeeUseCase {

	/**
	 * Soft-deletes an employee of the business owned by {@code ownerId}: its status becomes
	 * {@code DELETED}, and the row and its service assignments are kept.
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the employee
	 * doesn't exist, belongs to another business or is already deleted
	 * @throws com.micita.micita_project.core.employee.domain.exceptions.EmployeeHasFutureAppointmentsException
	 * if the employee has future appointments
	 */
	void delete(UUID ownerId, UUID employeeId);

}
