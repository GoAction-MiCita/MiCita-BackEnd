package com.micita.micita_project.core.auth.application.ports.out.query;

import java.util.UUID;

public interface BusinessQueryPort {

	/**
	 * @return whether the user already owns a business, which drives the {@code hasBusiness} claim
	 */
	boolean existsForOwner(UUID userId);

}
