package com.micita.micita_project.core.service.application.ports.in;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Changes the employees that provide a service (HU-09, HU-12). {@code service_employees} belongs to the
 * service module (ADR-002); employees are validated through the employee module.
 */
public interface AssignServiceEmployeesUseCase {

	/**
	 * Replaces the service's employees with {@code employeeIds}, ignoring repeated ids. Runs in the
	 * caller's transaction.
	 * @return the employees now assigned, in the order received
	 * @throws com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException if any
	 * employee is unknown, of another business or deleted; nothing is changed then
	 */
	Set<UUID> replaceEmployeesOfService(UUID businessId, UUID serviceId, Collection<UUID> employeeIds);

}
