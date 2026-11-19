package com.micita.micita_project.core.service.application.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.in.BookableServicesQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceQueryHandler implements BookableServicesQueryPort {

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	@Override
	public Set<UUID> findBookableIds(UUID businessId, Collection<UUID> serviceIds) {
		if (serviceIds.isEmpty()) {
			return Set.of();
		}
		return serviceQueryRepository.findBookableIds(businessId, Set.copyOf(serviceIds));
	}

}
