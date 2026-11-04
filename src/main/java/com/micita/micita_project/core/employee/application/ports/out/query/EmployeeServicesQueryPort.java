package com.micita.micita_project.core.employee.application.ports.out.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;

/**
 * Services of employees, read through the service module (ADR-002).
 */
public interface EmployeeServicesQueryPort {

	/**
	 * @return for each employee id that has services, its non-deleted services
	 */
	Map<UUID, List<EmployeeServiceRef>> findActiveServicesOf(UUID businessId, Collection<UUID> employeeIds);

}
