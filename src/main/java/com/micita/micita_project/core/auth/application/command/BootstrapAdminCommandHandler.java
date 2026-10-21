package com.micita.micita_project.core.auth.application.command;

import java.util.Set;

import com.micita.micita_project.core.auth.application.ports.in.BootstrapAdminUseCase;
import com.micita.micita_project.core.auth.application.ports.out.PasswordHasherPort;
import com.micita.micita_project.core.auth.application.ports.out.command.UserCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.models.User;

import org.springframework.stereotype.Service;

/**
 * Seeds the ADMIN account configured through {@code micita.security.bootstrap-user.*}. It is the
 * only way to obtain an administrator until role management exists.
 */
@Service
public class BootstrapAdminCommandHandler implements BootstrapAdminUseCase {

	static final String ADMIN_ROLE = "ADMIN";

	private final UserQueryRepositoryPort userQueryRepository;

	private final UserCommandRepositoryPort userCommandRepository;

	private final PasswordHasherPort passwordHasher;

	public BootstrapAdminCommandHandler(UserQueryRepositoryPort userQueryRepository,
			UserCommandRepositoryPort userCommandRepository, PasswordHasherPort passwordHasher) {
		this.userQueryRepository = userQueryRepository;
		this.userCommandRepository = userCommandRepository;
		this.passwordHasher = passwordHasher;
	}

	@Override
	public void ensureAdmin(String username, String password) {
		if (userQueryRepository.existsByUsername(username)) {
			return;
		}
		// Placeholder profile data: the account exists only to administer the platform.
		userCommandRepository.save(new User(null, "Admin", "MiCita", "N/A", "0000000", username + "@micita.local",
				username, passwordHasher.hash(password), Set.of(ADMIN_ROLE)));
	}

}
