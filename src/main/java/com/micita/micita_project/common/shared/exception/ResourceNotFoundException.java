package com.micita.micita_project.common.shared.exception;

/**
 * A requested entity does not exist. Mapped to 404.
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

}
