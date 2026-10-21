package com.micita.micita_project.common.shared.exception;

/**
 * The caller could not be authenticated. Mapped to 401.
 */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}

}
