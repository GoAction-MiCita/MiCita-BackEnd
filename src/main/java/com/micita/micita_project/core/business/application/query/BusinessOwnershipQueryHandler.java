package com.micita.micita_project.core.business.application.query;

import java.util.UUID;

import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.business.application.ports.out.query.BusinessQueryRepositoryPort;

import org.springframework.stereotype.Service;

@Service
public class BusinessOwnershipQueryHandler implements BusinessOwnershipQueryUseCase {

	private final BusinessQueryRepositoryPort businessQueryRepository;

	public BusinessOwnershipQueryHandler(BusinessQueryRepositoryPort businessQueryRepository) {
		this.businessQueryRepository = businessQueryRepository;
	}

	@Override
	public boolean ownsBusiness(UUID userId) {
		return businessQueryRepository.existsByOwnerId(userId);
	}

}
