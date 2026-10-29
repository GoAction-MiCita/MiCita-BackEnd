package com.micita.micita_project.core.service.domain.exceptions;

import java.util.Collection;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidReferencesException;

/**
 * Some {@code serviceIds} don't exist, belong to another business or were deleted.
 */
public class InvalidServiceIdsException extends InvalidReferencesException {

	public InvalidServiceIdsException(Collection<UUID> rejectedIds) {
		super("serviceIds", rejectedIds);
	}

}
