package com.micita.micita_project.common.shared.exception;

import java.util.Map;

import lombok.Getter;

/**
 * A listing's query parameters are invalid: {@code page} or {@code size} outside its range, or a filter
 * value that isn't allowed. Mapped to 400 with an {@code errors} map.
 */
@Getter
public class InvalidPageRequestException extends RuntimeException {

	private final Map<String, String> errors;

	public InvalidPageRequestException(Map<String, String> errors) {
		super("Request validation failed");
		this.errors = Map.copyOf(errors);
	}

}
