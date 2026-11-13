package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.query;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.mappers.ServiceBDMapper;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceQueryJpaAdapter implements ServiceQueryRepositoryPort {

	private final ServiceJpaRepository serviceJpaRepository;

	private final ServiceBDMapper serviceBDMapper;

	@Override
	public Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> serviceIds) {
		return serviceJpaRepository.findIdsOfBusiness(businessId, serviceIds, ServiceStatus.DELETED);
	}

	@Override
	public boolean existsByName(UUID businessId, String name) {
		return serviceJpaRepository.existsByName(businessId, name, ServiceStatus.DELETED);
	}

	@Override
	public PageResponse<Service> findPageOfBusiness(UUID businessId, int page, int size) {
		Page<ServiceEntity> result = serviceJpaRepository.findByBusinessIdAndStatusNot(businessId,
				ServiceStatus.DELETED,
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"))));
		List<Service> content = result.getContent().stream().map(serviceBDMapper::toDomain).toList();
		return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(),
				result.getTotalPages());
	}

}
