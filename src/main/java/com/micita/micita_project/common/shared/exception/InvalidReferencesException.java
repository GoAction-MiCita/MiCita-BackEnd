package com.micita.micita_project.common.shared.exception;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.Getter;

/**
 * Some ids sent in {@code field} don't exist, belong to another business or were deleted. Mapped to
 * 400 with the field in {@code errors} and the offending ids in {@code rejectedIds}.
 */
@Getter
public class InvalidReferencesException extends RuntimeException {

	private final String field;

	private final List<UUID> rejectedIds;

	public InvalidReferencesException(String field, Collection<UUID> rejectedIds) {
		super("Some referenced ids are unknown or not available");
		this.field = field;
		this.rejectedIds = List.copyOf(rejectedIds);
	}

}
