package com.micita.micita_project.core.auth.domain.exceptions;

import com.micita.micita_project.common.shared.exception.ConflictException;

public class DuplicateUserException extends ConflictException {

	public DuplicateUserException(String message) {
		super(message);
	}

	public static DuplicateUserException email() {
		return new DuplicateUserException("Email is already registered");
	}

	public static DuplicateUserException username() {
		return new DuplicateUserException("Username is already registered");
	}

	public static DuplicateUserException emailOrUsername() {
		return new DuplicateUserException("Email or username is already registered");
	}

}
