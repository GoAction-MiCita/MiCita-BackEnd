package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.command;

import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.out.command.ServiceEmployeeCommandRepositoryPort;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEmployeeEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceEmployeeJpaRepository;

import org.springframework.stereotype.Component;

@Component
public class ServiceEmployeeCommandJpaAdapter implements ServiceEmployeeCommandRepositoryPort {

	private final ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	public ServiceEmployeeCommandJpaAdapter(ServiceEmployeeJpaRepository serviceEmployeeJpaRepository) {
		this.serviceEmployeeJpaRepository = serviceEmployeeJpaRepository;
	}

	@Override
	public void replaceServicesOfEmployee(UUID employeeId, Set<UUID> serviceIds) {
		serviceEmployeeJpaRepository.deleteByEmployeeId(employeeId);
		serviceEmployeeJpaRepository
			.saveAll(serviceIds.stream().map(serviceId -> new ServiceEmployeeEntity(serviceId, employeeId)).toList());
	}

}
