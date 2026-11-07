package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.query;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.out.query.ServiceEmployeeQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.ServiceAssignment;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceEmployeeJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceEmployeeQueryJpaAdapter implements ServiceEmployeeQueryRepositoryPort {

	private final ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Override
	public List<ServiceAssignment> findActiveAssignments(UUID businessId, Collection<UUID> employeeIds) {
		return serviceEmployeeJpaRepository.findActiveAssignments(businessId, employeeIds, ServiceStatus.DELETED);
	}

	@Override
	public Set<UUID> findEmployeeIdsAssignedTo(Collection<UUID> serviceIds) {
		return serviceEmployeeJpaRepository.findEmployeeIdsAssignedTo(serviceIds);
	}

}
