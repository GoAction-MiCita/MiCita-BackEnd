package com.micita.micita_project.core.service.application.ports.out.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.domain.models.Service;

public interface ServiceQueryRepositoryPort {

	/**
	 * @return the ids among {@code serviceIds} that belong to the business and are not deleted
	 */
	Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> serviceIds);

	/**
	 * @return whether a non-deleted service of the business already has {@code name}, ignoring case
	 */
	boolean existsByName(UUID businessId, String name);

	/**
	 * Services of the business that are not deleted, by name ignoring case (then by id).
	 */
	PageResponse<Service> findPageOfBusiness(UUID businessId, int page, int size);

}
