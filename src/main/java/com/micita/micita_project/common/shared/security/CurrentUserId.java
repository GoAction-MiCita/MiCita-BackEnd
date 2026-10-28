package com.micita.micita_project.common.shared.security;

import java.util.UUID;

import com.micita.micita_project.common.shared.exception.UnauthorizedException;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads the authenticated user's id from the {@code uid} claim. Anything that belongs to a user
 * (such as the business) is resolved from here, never from a request parameter.
 */
public final class CurrentUserId {

	private CurrentUserId() {
	}

	/**
	 * @throws UnauthorizedException if the token has no valid user id (e.g. it predates the claim)
	 */
	public static UUID from(Jwt jwt) {
		String userId = jwt.getClaimAsString(SecurityConfig.USER_ID_CLAIM);
		if (userId == null) {
			throw missingUserId();
		}
		try {
			return UUID.fromString(userId);
		}
		catch (IllegalArgumentException ex) {
			throw missingUserId();
		}
	}

	private static UnauthorizedException missingUserId() {
		return new UnauthorizedException("Access token has no valid user id: log in again");
	}

}
