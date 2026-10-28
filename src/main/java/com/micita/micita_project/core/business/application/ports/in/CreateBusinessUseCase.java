package com.micita.micita_project.core.business.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.core.business.application.dto.request.CreateBusinessRequest;
import com.micita.micita_project.core.business.application.dto.response.BusinessResponse;

public interface CreateBusinessUseCase {

	/**
	 * Registers the business of {@code ownerId}, the authenticated user.
	 * @throws com.micita.micita_project.core.business.domain.exceptions.BusinessAlreadyExistsException
	 * if the user already has one
	 */
	BusinessResponse create(UUID ownerId, CreateBusinessRequest request);

}
