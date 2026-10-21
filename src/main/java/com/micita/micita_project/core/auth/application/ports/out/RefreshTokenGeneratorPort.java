package com.micita.micita_project.core.auth.application.ports.out;

public interface RefreshTokenGeneratorPort {

	/**
	 * @return a new random, opaque refresh token value
	 */
	String generate();

	/**
	 * @return the deterministic hash under which the token is stored and looked up
	 */
	String hash(String rawToken);

}
