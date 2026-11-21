package com.micita.micita_project.core.service.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.service.application.dto.response.ServiceDetailResponse;

/**
 * HU-10: the detail of a service of the caller's business.
 */
public interface GetServiceUseCase {

	/**
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the service
	 * doesn't exist, belongs to another business or is deleted
	 */
	ServiceDetailResponse get(UUID ownerId, UUID serviceId);

}
