package com.micita.micita_project.core.service.application.ports.in;

import java.util.UUID;

public interface DeleteServiceUseCase {

	/**
	 * Soft-deletes a service of the business owned by {@code ownerId}: its status becomes {@code DELETED},
	 * and the row and its employee assignments are kept.
	 * @throws com.micita.micita_project.common.shared.exception.ResourceNotFoundException if the service
	 * doesn't exist, belongs to another business or is already deleted
	 * @throws com.micita.micita_project.core.service.domain.exceptions.ServiceHasFutureAppointmentsException
	 * if the service has future appointments
	 */
	void delete(UUID ownerId, UUID serviceId);

}
