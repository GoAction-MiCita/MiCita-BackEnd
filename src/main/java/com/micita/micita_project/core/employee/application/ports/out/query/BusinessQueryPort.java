package com.micita.micita_project.core.employee.application.ports.out.query;

import java.util.UUID;

public interface BusinessQueryPort {

	/**
	 * @return the id of the business the user owns; employees always belong to it
	 * @throws com.micita.micita_project.common.shared.exception.ForbiddenException if the user has no
	 * business
	 */
	UUID requireBusinessIdOf(UUID userId);

}
