package com.micita.micita_project.common.shared.exception;

/**
 * The caller is authenticated but may not do this. Mapped to 403 with a machine-readable
 * {@code code} so the frontend can tell the reasons apart.
 */
public class ForbiddenException extends RuntimeException {

	private final String code;

	public ForbiddenException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}

}
