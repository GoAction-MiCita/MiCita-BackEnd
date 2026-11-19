package com.micita.micita_project.core.service.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.service.application.dto.request.ChangeServiceStatusRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceStatusResponse;

public interface ChangeServiceStatusUseCase {

	/**
	 * Activates or deactivates a service of the business owned by {@code ownerId}. Asking for the current
	 * status changes nothing.
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the service
	 * doesn't exist, belongs to another business or is deleted
	 */
	ServiceStatusResponse changeStatus(UUID ownerId, UUID serviceId, ChangeServiceStatusRequest request);

}
