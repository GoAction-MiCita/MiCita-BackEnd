package com.micita.micita_project.core.service.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.service.application.dto.request.CreateServiceRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;

public interface CreateServiceUseCase {

	/**
	 * Creates a service in the business of {@code ownerId}, assigning {@code employeeIds} if present.
	 * @throws com.micita.micita_project.core.service.domain.exceptions.DuplicateServiceException if another
	 * non-deleted service of the business has the name, ignoring case
	 * @throws com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException if any
	 * employee is unknown, of another business or deleted; nothing is stored then
	 */
	ServiceResponse create(UUID ownerId, CreateServiceRequest request);

}
