package com.micita.micita_project.core.employee.application.ports.out.command;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Assigns services to an employee. The association belongs to the service module (ADR-002).
 */
public interface EmployeeServicesCommandPort {

	/**
	 * @return the services now assigned, without repeats
	 * @throws com.micita.micita_project.common.shared.exception.InvalidReferencesException if any service
	 * is unknown, of another business or deleted
	 */
	Set<UUID> replaceServices(UUID businessId, UUID employeeId, Collection<UUID> serviceIds);

}
