package com.micita.micita_project.core.service.application.ports.in;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	/**
	 * Employees that provide at least one of {@code serviceIds}, used to filter the employee listing
	 * (HU-22). The result may include deleted employees; the caller filters them out.
	 * @return the ids of those employees, empty if {@code serviceIds} is empty
	 * @throws com.micita.micita_project.core.service.domain.exceptions.InvalidServiceIdsException if any
	 * service is unknown, of another business or deleted
	 */
	Set<UUID> findEmployeeIdsProviding(UUID businessId, Collection<UUID> serviceIds);

	record ServiceSummary(UUID id, String name) {

	}

}
