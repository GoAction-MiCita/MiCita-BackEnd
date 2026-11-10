package com.micita.micita_project.core.service.application.ports.out.command;

import com.micita.micita_project.core.service.domain.models.Service;

public interface ServiceCommandRepositoryPort {

	/**
	 * @return the stored service, with its generated id
	 */
	Service save(Service service);

}
