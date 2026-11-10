package com.micita.micita_project.core.service.application.ports.out.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface EmployeeQueryPort {

	/**
	 * @return the ids among {@code employeeIds} that exist in the business and are not deleted
	 */
	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds);

}
