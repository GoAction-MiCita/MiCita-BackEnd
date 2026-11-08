package com.micita.micita_project.common.shared.exception;

import java.util.Map;

import lombok.Getter;

/**
 * The request clashes with current state (duplicate name, slot already taken...).
 * Mapped to 409. {@code properties} are added to the {@code ProblemDetail} as extra members.
 */
@Getter
public class ConflictException extends RuntimeException {

	private final Map<String, Object> properties;

	public ConflictException(String message) {
		this(message, Map.of());
	}

	public ConflictException(String message, Map<String, Object> properties) {
		super(message);
		this.properties = Map.copyOf(properties);
	}

}
