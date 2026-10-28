package com.micita.micita_project.core.business.domain.exceptions;

import com.micita.micita_project.common.shared.exception.ConflictException;

public class BusinessAlreadyExistsException extends ConflictException {

	public BusinessAlreadyExistsException() {
		super("User already has a business");
	}

}
