package com.micita.micita_project.core.auth.application.ports.out;

import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;

public interface CredentialsAuthenticatorPort {

	/**
	 * @throws com.micita.micita_project.core.auth.domain.exceptions.InvalidCredentialsException if
	 * the credentials are invalid
	 */
	AuthenticatedUser authenticate(String username, String password);

}
