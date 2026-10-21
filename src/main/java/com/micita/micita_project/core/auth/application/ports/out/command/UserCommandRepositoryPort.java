package com.micita.micita_project.core.auth.application.ports.out.command;

import com.micita.micita_project.core.auth.domain.models.User;

public interface UserCommandRepositoryPort {

	/**
	 * @return the stored user, with its generated id
	 * @throws com.micita.micita_project.core.auth.domain.exceptions.DuplicateUserException if the
	 * email or username is already taken
	 */
	User save(User user);

}
