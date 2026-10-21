package com.micita.micita_project.core.auth.infrastructure.adapters.out.business;

import java.util.UUID;

import com.micita.micita_project.core.auth.application.ports.out.query.BusinessQueryPort;

import org.springframework.stereotype.Component;

/**
 * Placeholder until the businesses module (onboarding, {@code POST /api/negocios/onboarding})
 * exists: nobody owns a business yet, so {@code hasBusiness} is always false. Replace it with an
 * adapter that queries the businesses table.
 */
@Component
public class PendingBusinessQueryAdapter implements BusinessQueryPort {

	@Override
	public boolean existsForOwner(UUID userId) {
		return false;
	}

}
