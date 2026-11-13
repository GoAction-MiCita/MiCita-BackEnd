package com.micita.micita_project.core.service.domain.models;

import java.util.Set;
import java.util.UUID;

/**
 * Restrictions on the service listing (HU-13), combined with AND. A {@code null} field doesn't restrict.
 * {@code name} matches ignoring case; {@code serviceIds} limits the result to those ids. Deleted services
 * are always left out.
 */
public record ServiceCriteria(String name, ServiceStatus status, Set<UUID> serviceIds) {

	public static ServiceCriteria none() {
		return new ServiceCriteria(null, null, null);
	}

}
