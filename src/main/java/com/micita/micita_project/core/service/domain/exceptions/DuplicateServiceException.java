package com.micita.micita_project.core.service.domain.exceptions;

import com.micita.micita_project.common.shared.exception.ConflictException;

/**
 * Another service of the same business, not deleted, already has the name (compared ignoring case).
 */
public class DuplicateServiceException extends ConflictException {

	public DuplicateServiceException() {
		super("Another service of the business already uses this name");
	}

}
