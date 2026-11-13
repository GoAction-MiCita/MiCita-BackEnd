package com.micita.micita_project.core.service.application.ports.out.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceCriteria;

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
	 * Services of the business that are not deleted and match {@code criteria}, by name ignoring case (then
	 * by id). The page totals count only the matching services.
	 */
	PageResponse<Service> findPageOfBusiness(UUID businessId, ServiceCriteria criteria, int page, int size);

}
