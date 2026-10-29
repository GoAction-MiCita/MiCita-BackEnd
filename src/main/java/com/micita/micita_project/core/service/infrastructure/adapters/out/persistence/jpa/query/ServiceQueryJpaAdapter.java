package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceQueryJpaAdapter implements ServiceQueryRepositoryPort {

	private final ServiceJpaRepository serviceJpaRepository;

	@Override
	public Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> serviceIds) {
		return serviceJpaRepository.findIdsOfBusiness(businessId, serviceIds, ServiceStatus.DELETED);
	}

}
