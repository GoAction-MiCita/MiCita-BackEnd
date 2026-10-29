package com.micita.micita_project.core.auth.infrastructure.adapters.out.business;

import java.util.UUID;

import com.micita.micita_project.core.auth.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Answers {@code hasBusiness} through the business module's use case, so login and refresh reflect
 * what is stored in PostgreSQL.
 */
@Component
@RequiredArgsConstructor
public class BusinessModuleQueryAdapter implements BusinessQueryPort {

	private final BusinessOwnershipQueryUseCase businessOwnershipQuery;

	@Override
	public boolean existsForOwner(UUID userId) {
		return businessOwnershipQuery.ownsBusiness(userId);
	}

}
