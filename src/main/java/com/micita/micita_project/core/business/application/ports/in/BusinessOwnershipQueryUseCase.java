package com.micita.micita_project.core.business.application.ports.in;

import java.util.UUID;

public interface BusinessOwnershipQueryUseCase {

	boolean ownsBusiness(UUID userId);

	/**
	 * @return the id of the business the user owns, for modules that scope their data by business
	 * @throws com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException if the
	 * user has no business yet
	 */
	UUID requireBusinessIdOf(UUID userId);

}
