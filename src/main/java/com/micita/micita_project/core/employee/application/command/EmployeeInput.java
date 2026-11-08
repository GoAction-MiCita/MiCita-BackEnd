package com.micita.micita_project.core.employee.application.command;

import java.util.Locale;

/**
 * Normalization shared by registration (HU-17) and edition (HU-18).
 */
final class EmployeeInput {

	private EmployeeInput() {
	}

	/** Optional fields sent blank are stored as absent. */
	static String optional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	/** Emails are stored lowercased, so uniqueness ignores case. */
	static String email(String value) {
		String email = optional(value);
		return email == null ? null : email.toLowerCase(Locale.ROOT);
	}

}
