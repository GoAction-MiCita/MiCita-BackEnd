package com.micita.micita_project.core.service.application.ports.out.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface ServiceQueryRepositoryPort {

	/**
	 * @return the ids among {@code serviceIds} that belong to the business and are not deleted
	 */
	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> serviceIds);

}
