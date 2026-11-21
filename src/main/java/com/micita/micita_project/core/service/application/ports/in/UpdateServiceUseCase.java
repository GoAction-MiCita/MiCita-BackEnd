package com.micita.micita_project.core.service.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.service.application.dto.request.UpdateServiceRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;

/**
 * HU-10: replaces the data and the employees of a service of the caller's business.
 */
public interface UpdateServiceUseCase {

	/**
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the service
	 * doesn't exist, belongs to another business or is deleted
	 * @throws com.micita.micita_project.core.service.domain.exceptions.DuplicateServiceException if another
	 * non-deleted service of the business uses the name
	 * @throws com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException if any
	 * employee is unknown, of another business or deleted; nothing is changed then
	 */
	ServiceResponse update(UUID ownerId, UUID serviceId, UpdateServiceRequest request);

}
