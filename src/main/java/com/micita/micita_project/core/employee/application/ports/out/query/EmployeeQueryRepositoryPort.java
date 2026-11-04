package com.micita.micita_project.core.employee.application.ports.out.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.domain.models.Employee;

/**
 * Lookups within one business. Deleted employees are ignored everywhere.
 */
public interface EmployeeQueryRepositoryPort {

	boolean existsByEmail(UUID businessId, String email);

	boolean existsByPhone(UUID businessId, String phone);

	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds);

	/**
	 * Employees that are not deleted, newest first.
	 */
	PageResponse<Employee> findPageOfBusiness(UUID businessId, int page, int size);

	/**
	 * @return the ids among {@code employeeIds} that are {@code ACTIVE} in the business
	 */
	Set<UUID> findBookableIds(UUID businessId, Collection<UUID> employeeIds);

}
