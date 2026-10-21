package com.micita.micita_project.core.auth.domain.exceptions;

import com.micita.micita_project.common.shared.exception.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {

	public InvalidCredentialsException() {
		super("Invalid credentials");
	}

}
