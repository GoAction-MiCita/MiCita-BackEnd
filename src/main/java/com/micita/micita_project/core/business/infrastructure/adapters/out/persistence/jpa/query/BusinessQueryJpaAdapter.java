package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.query;

import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.core.business.application.ports.out.query.BusinessQueryRepositoryPort;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories.BusinessJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessQueryJpaAdapter implements BusinessQueryRepositoryPort {

	private final BusinessJpaRepository businessJpaRepository;

	@Override
	public boolean existsByOwnerId(UUID ownerId) {
		return businessJpaRepository.existsByOwnerId(ownerId);
	}

	@Override
	public Optional<UUID> findIdByOwnerId(UUID ownerId) {
		return businessJpaRepository.findIdByOwnerId(ownerId);
	}

}
