package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.command;

import com.micita.micita_project.core.service.application.ports.out.command.ServiceCommandRepositoryPort;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.mappers.ServiceBDMapper;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceCommandJpaAdapter implements ServiceCommandRepositoryPort {

	private final ServiceJpaRepository serviceJpaRepository;

	private final ServiceBDMapper serviceBDMapper;

	@Override
	public Service save(Service service) {
		return serviceBDMapper.toDomain(serviceJpaRepository.save(serviceBDMapper.toEntity(service)));
	}

}
