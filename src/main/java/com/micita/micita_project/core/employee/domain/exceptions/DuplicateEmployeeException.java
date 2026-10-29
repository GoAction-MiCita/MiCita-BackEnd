package com.micita.micita_project.core.employee.domain.exceptions;

import com.micita.micita_project.common.shared.exception.ConflictException;

/**
 * Another employee of the same business, not deleted, already uses the email or phone.
 */
public class DuplicateEmployeeException extends ConflictException {

	private DuplicateEmployeeException(String message) {
		super(message);
	}

	public static DuplicateEmployeeException email() {
		return new DuplicateEmployeeException("Another employee of the business already uses this email");
	}

	public static DuplicateEmployeeException phone() {
		return new DuplicateEmployeeException("Another employee of the business already uses this phone");
	}

}
