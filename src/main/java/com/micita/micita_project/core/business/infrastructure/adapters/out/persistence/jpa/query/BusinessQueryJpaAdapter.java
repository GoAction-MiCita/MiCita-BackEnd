package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.query;

import java.util.UUID;

import com.micita.micita_project.core.business.application.ports.out.query.BusinessQueryRepositoryPort;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories.BusinessJpaRepository;

import org.springframework.stereotype.Component;

@Component
public class BusinessQueryJpaAdapter implements BusinessQueryRepositoryPort {

	private final BusinessJpaRepository businessJpaRepository;

	public BusinessQueryJpaAdapter(BusinessJpaRepository businessJpaRepository) {
		this.businessJpaRepository = businessJpaRepository;
	}

	@Override
	public boolean existsByOwnerId(UUID ownerId) {
		return businessJpaRepository.existsByOwnerId(ownerId);
	}

}
