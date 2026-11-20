package com.micita.micita_project.core.service.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.service.application.dto.request.ReplaceServiceEmployeesRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceEmployeesResponse;

/**
 * HU-12: replaces the employees of a service of the caller's business.
 */
public interface ReplaceServiceEmployeesUseCase {

	/**
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the service
	 * doesn't exist, belongs to another business or is deleted
	 * @throws com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException if any
	 * employee is unknown, of another business or deleted; nothing is changed then
	 */
	ServiceEmployeesResponse replaceEmployees(UUID ownerId, UUID serviceId, ReplaceServiceEmployeesRequest request);

}
