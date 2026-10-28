package com.micita.micita_project.core.business.application.ports.out.command;

import com.micita.micita_project.core.business.domain.models.Business;

public interface BusinessCommandRepositoryPort {

	/**
	 * @return the stored business, with its generated id
	 * @throws com.micita.micita_project.core.business.domain.exceptions.BusinessAlreadyExistsException
	 * if the owner already has a business
	 */
	Business save(Business business);

}
