package com.micita.micita_project.core.employee.application.ports.out.query;

import java.util.Collection;
import java.util.List;
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
	 * Like {@link #existsByEmail} but ignoring {@code employeeId}, so an employee doesn't clash with itself.
	 */
	boolean existsByEmailExcept(UUID businessId, String email, UUID employeeId);

	/**
	 * Like {@link #existsByPhone} but ignoring {@code employeeId}, so an employee doesn't clash with itself.
	 */
	boolean existsByPhoneExcept(UUID businessId, String phone, UUID employeeId);

	/**
	 * @return the employee if it belongs to the business and is not deleted
	 */
	Optional<Employee> findOfBusiness(UUID businessId, UUID employeeId);

	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds);

	/**
	 * @return the employees among {@code employeeIds} that belong to the business and are not deleted
	 */
	List<Employee> findAllOfBusiness(UUID businessId, Collection<UUID> employeeIds);

	/**
	 * Employees that are not deleted and match {@code criteria}, newest first.
	 */
	PageResponse<Employee> findPageOfBusiness(UUID businessId, EmployeeCriteria criteria, int page, int size);

	/**
	 * @return the ids among {@code employeeIds} that are {@code ACTIVE} in the business
	 */
	Set<UUID> findBookableIds(UUID businessId, Collection<UUID> employeeIds);

}
