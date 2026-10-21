package com.micita.micita_project.core.auth.application.ports.in;

public interface BootstrapAdminUseCase {

	/**
	 * Creates the bootstrap ADMIN account unless a user with that username already exists.
	 */
	void ensureAdmin(String username, String password);

}
