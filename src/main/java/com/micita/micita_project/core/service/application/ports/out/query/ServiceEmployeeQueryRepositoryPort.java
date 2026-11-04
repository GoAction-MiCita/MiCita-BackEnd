package com.micita.micita_project.core.service.application.ports.out.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.micita.micita_project.core.service.domain.models.ServiceAssignment;

public interface ServiceEmployeeQueryRepositoryPort {

	/**
	 * Assignments of the given employees to services of the business that are not deleted.
	 */
	List<ServiceAssignment> findActiveAssignments(UUID businessId, Collection<UUID> employeeIds);

}
