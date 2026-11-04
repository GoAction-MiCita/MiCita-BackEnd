package com.micita.micita_project.common.shared.exception;

import java.util.Map;

import lombok.Getter;

/**
 * {@code page} or {@code size} is outside its range. Mapped to 400 with an {@code errors} map.
 */
@Getter
public class InvalidPageRequestException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidPageRequestException(Map<String, String> errors) {
		super("Request validation failed");
		this.errors = Map.copyOf(errors);
	}

}
