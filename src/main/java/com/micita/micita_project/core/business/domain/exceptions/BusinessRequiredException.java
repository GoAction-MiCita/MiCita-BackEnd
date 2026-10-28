package com.micita.micita_project.core.business.domain.exceptions;

import com.micita.micita_project.common.shared.exception.ForbiddenException;

/**
 * The user has not completed onboarding, so it cannot use endpoints that need a business (HU-04).
 */
public class BusinessRequiredException extends ForbiddenException {

	public static final String CODE = "BUSINESS_REQUIRED";

	public BusinessRequiredException() {
		super(CODE, "Register your business before using this feature");
	}

}
