package com.micita.micita_project.core.employee.application.ports.in;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * What other modules may ask about employees without touching their tables (ADR-002). The service
 * module uses it to validate {@code employeeIds} (HU-12).
 */
public interface EmployeeQueryUseCase {

	/**
	 * @return the ids among {@code employeeIds} that exist in the business and are not deleted
	 */
	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds);

}
