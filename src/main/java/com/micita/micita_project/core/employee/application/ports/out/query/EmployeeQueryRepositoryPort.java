package com.micita.micita_project.core.employee.application.ports.out.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Lookups within one business. Deleted employees are ignored everywhere.
 */
public interface EmployeeQueryRepositoryPort {

	boolean existsByEmail(UUID businessId, String email);

	boolean existsByPhone(UUID businessId, String phone);

	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds);

}
