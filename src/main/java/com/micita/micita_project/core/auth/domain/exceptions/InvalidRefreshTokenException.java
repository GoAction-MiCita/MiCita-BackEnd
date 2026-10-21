package com.micita.micita_project.core.auth.domain.exceptions;

import com.micita.micita_project.common.shared.exception.UnauthorizedException;

/**
 * The refresh token is unknown, expired or revoked. The client must log in again.
 */
public class InvalidRefreshTokenException extends UnauthorizedException {

	public InvalidRefreshTokenException() {
		super("Invalid or expired refresh token");
	}

}
