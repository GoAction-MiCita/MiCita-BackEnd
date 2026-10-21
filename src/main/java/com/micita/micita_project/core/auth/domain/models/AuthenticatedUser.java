package com.micita.micita_project.core.auth.domain.models;

import java.util.List;
import java.util.UUID;

/**
 * A user whose identity has been proven (by password or refresh token). {@code roles} hold bare
 * role names such as {@code ADMIN}; the token issuer adds the {@code ROLE_} prefix.
 */
public record AuthenticatedUser(UUID userId, String username, List<String> roles) {

	public AuthenticatedUser {
		roles = List.copyOf(roles);
	}

	public static AuthenticatedUser of(User user) {
		return new AuthenticatedUser(user.id(), user.username(), user.roles().stream().sorted().toList());
	}

}
