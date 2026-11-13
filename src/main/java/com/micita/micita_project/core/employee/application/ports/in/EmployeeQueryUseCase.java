package com.micita.micita_project.core.employee.application.ports.in;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.domain.models.EmployeeSummary;

/**
 * What other modules may ask about employees without touching their tables (ADR-002). The service
 * module uses it to validate {@code employeeIds} (HU-12) and to show employees in its listing (HU-08).
 */
public interface EmployeeQueryUseCase {

	/**
	 * @return the ids among {@code employeeIds} that exist in the business and are not deleted
	 */
	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds);

	/**
	 * @return who the employees among {@code employeeIds} are, leaving out unknown, foreign and deleted ones
	 */
	List<EmployeeSummary> findSummaries(UUID businessId, Collection<UUID> employeeIds);

}
