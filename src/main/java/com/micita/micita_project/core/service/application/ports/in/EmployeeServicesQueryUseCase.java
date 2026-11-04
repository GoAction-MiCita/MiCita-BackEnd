package com.micita.micita_project.core.service.application.ports.in;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read access to {@code service_employees} for other modules (ADR-002): the employee module lists
 * the services of its employees through it.
 */
public interface EmployeeServicesQueryUseCase {

	/**
	 * @return for each employee id that has services, its non-deleted services ordered by name
	 */
	Map<UUID, List<ServiceSummary>> findActiveServicesOf(UUID businessId, Collection<UUID> employeeIds);

	record ServiceSummary(UUID id, String name) {

	}

}
