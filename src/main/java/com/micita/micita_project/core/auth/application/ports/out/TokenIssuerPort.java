package com.micita.micita_project.core.auth.application.ports.out;

import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.IssuedToken;

public interface TokenIssuerPort {

	/**
	 * Issues a short-lived access token. {@code hasBusiness} tells the frontend whether to send the
	 * user to onboarding.
	 */
	IssuedToken issue(AuthenticatedUser user, boolean hasBusiness);

}
