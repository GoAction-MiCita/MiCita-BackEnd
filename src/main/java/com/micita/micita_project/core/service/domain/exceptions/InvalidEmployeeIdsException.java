package com.micita.micita_project.core.service.domain.exceptions;

import java.util.Collection;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidReferencesException;

/**
 * Some {@code employeeIds} don't exist, belong to another business or were deleted.
 */
public class InvalidEmployeeIdsException extends InvalidReferencesException {

	public InvalidEmployeeIdsException(Collection<UUID> rejectedIds) {
		super("employeeIds", rejectedIds);
	}

}
