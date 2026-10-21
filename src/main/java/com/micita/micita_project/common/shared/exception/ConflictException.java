package com.micita.micita_project.common.shared.exception;

/**
 * The request clashes with current state (duplicate name, slot already taken...).
 * Mapped to 409.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
