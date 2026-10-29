package com.micita.micita_project.core.service.application.ports.in;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * The only way to change the services an employee provides: {@code service_employees} belongs to the
 * service module (ADR-002), so the employee module goes through this port.
 */
public interface AssignEmployeeServicesUseCase {

	/**
	 * Replaces the employee's services with {@code serviceIds}, ignoring repeated ids. Runs in the
	 * caller's transaction.
	 * @return the services now assigned, in the order received
	 * @throws com.micita.micita_project.core.service.domain.exceptions.InvalidServiceIdsException if any
	 * service is unknown, of another business or deleted; nothing is changed then
	 */
	Set<UUID> replaceServicesOfEmployee(UUID businessId, UUID employeeId, Collection<UUID> serviceIds);

}
