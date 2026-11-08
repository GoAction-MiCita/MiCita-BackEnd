package com.micita.micita_project.core.employee.application.ports.out.query;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeCriteria;

/**
 * Lookups within one business. Deleted employees are ignored everywhere.
 */
public interface EmployeeQueryRepositoryPort {

	boolean existsByEmail(UUID businessId, String email);

	boolean existsByPhone(UUID businessId, String phone);

	/**
	 * @return the employee if it belongs to the business and is not deleted
	 */
	Optional<Employee> findOfBusiness(UUID businessId, UUID employeeId);

	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds);

	/**
	 * Employees that are not deleted and match {@code criteria}, newest first.
	 */
	PageResponse<Employee> findPageOfBusiness(UUID businessId, EmployeeCriteria criteria, int page, int size);

	/**
	 * @return the ids among {@code employeeIds} that are {@code ACTIVE} in the business
	 */
	Set<UUID> findBookableIds(UUID businessId, Collection<UUID> employeeIds);

}
