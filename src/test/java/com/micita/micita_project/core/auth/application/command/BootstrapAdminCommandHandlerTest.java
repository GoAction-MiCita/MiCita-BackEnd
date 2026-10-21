package com.micita.micita_project.core.auth.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Set;

import com.micita.micita_project.core.auth.application.ports.out.PasswordHasherPort;
import com.micita.micita_project.core.auth.application.ports.out.command.UserCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminCommandHandlerTest {

	@Mock
	private UserQueryRepositoryPort userQueryRepository;

	@Mock
	private UserCommandRepositoryPort userCommandRepository;

	@Mock
	private PasswordHasherPort passwordHasher;

	@InjectMocks
	private BootstrapAdminCommandHandler bootstrapAdminCommandHandler;

	@Test
	void ensureAdminCreatesAdminWhenMissing() {
		given(passwordHasher.hash("admin-pass")).willReturn("{bcrypt}hash");

		bootstrapAdminCommandHandler.ensureAdmin("admin", "admin-pass");

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userCommandRepository).save(saved.capture());
		assertThat(saved.getValue().username()).isEqualTo("admin");
		assertThat(saved.getValue().email()).isEqualTo("admin@micita.local");
		assertThat(saved.getValue().passwordHash()).isEqualTo("{bcrypt}hash");
		assertThat(saved.getValue().roles()).isEqualTo(Set.of("ADMIN"));
	}

	@Test
	void ensureAdminLeavesExistingUserUntouched() {
		given(userQueryRepository.existsByUsername("admin")).willReturn(true);

		bootstrapAdminCommandHandler.ensureAdmin("admin", "admin-pass");

		verify(userCommandRepository, never()).save(any());
	}

}
