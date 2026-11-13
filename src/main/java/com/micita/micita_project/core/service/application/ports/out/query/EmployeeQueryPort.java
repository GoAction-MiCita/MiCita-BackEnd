package com.micita.micita_project.core.service.application.ports.out.query;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.domain.models.ServiceEmployee;

public interface EmployeeQueryPort {

	/**
	 * @return the ids among {@code employeeIds} that exist in the business and are not deleted
	 */
	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds);

	/**
	 * @return who the employees among {@code employeeIds} are, leaving out unknown, foreign and deleted ones
	 */
	List<ServiceEmployee> findEmployees(UUID businessId, Collection<UUID> employeeIds);

}
