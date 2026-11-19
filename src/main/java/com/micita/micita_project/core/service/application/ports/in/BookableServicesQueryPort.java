package com.micita.micita_project.core.service.application.ports.in;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Which services can be booked (F2). Only {@code ACTIVE} ones are bookable: inactive and deleted
 * services never are.
 */
public interface BookableServicesQueryPort {

	/**
	 * @return the ids among {@code serviceIds} that belong to the business and are {@code ACTIVE}
	 */
	Set<UUID> findBookableIds(UUID businessId, Collection<UUID> serviceIds);

}
