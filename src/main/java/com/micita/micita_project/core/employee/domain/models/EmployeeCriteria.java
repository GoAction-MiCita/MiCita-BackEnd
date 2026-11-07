package com.micita.micita_project.core.employee.domain.models;

import java.util.Set;
import java.util.UUID;

/**
 * Restrictions on the employee listing (HU-22), combined with AND. A {@code null} field doesn't
 * restrict. {@code name} matches the first or last name, ignoring case; {@code employeeIds} limits the
 * result to those ids. Deleted employees are always left out.
 */
public record EmployeeCriteria(String name, EmployeeStatus status, Set<UUID> employeeIds) {

	public static EmployeeCriteria none() {
		return new EmployeeCriteria(null, null, null);
	}

}
