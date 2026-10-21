package com.micita.micita_project.core.auth.domain.models;

import java.util.Set;
import java.util.UUID;

/**
 * A platform account. {@code roles} hold bare role names (e.g. {@code ADMIN}), without the
 * {@code ROLE_} prefix. A {@code null} id means the user has not been persisted yet.
 */
public record User(UUID id, String firstName, String lastName, String address, String phone, String email,
		String username, String passwordHash, Set<String> roles) {

	public User {
		roles = Set.copyOf(roles);
	}

}
