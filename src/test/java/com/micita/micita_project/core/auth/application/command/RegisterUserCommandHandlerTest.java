package com.micita.micita_project.core.auth.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.auth.application.dto.request.RegisterUserRequest;
import com.micita.micita_project.core.auth.application.dto.response.UserResponse;
import com.micita.micita_project.core.auth.application.mappers.UserMapper;
import com.micita.micita_project.core.auth.application.ports.out.PasswordHasherPort;
import com.micita.micita_project.core.auth.application.ports.out.command.UserCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.exceptions.DuplicateUserException;
import com.micita.micita_project.core.auth.domain.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

	private static final UUID ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	@Mock
	private UserQueryRepositoryPort userQueryRepository;

	@Mock
	private UserCommandRepositoryPort userCommandRepository;

	@Mock
	private PasswordHasherPort passwordHasher;

	@Spy
	private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

	@InjectMocks
	private RegisterUserCommandHandler registerUserCommandHandler;

	@Test
	void registerStoresHashedPasswordAndNoRoles() {
		given(passwordHasher.hash("Secret#1")).willReturn("{bcrypt}hash");
		given(userCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));

		UserResponse response = registerUserCommandHandler.register(request("Ana@Example.com "));

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userCommandRepository).save(saved.capture());
		assertThat(saved.getValue().id()).isNull();
		assertThat(saved.getValue().passwordHash()).isEqualTo("{bcrypt}hash");
		assertThat(saved.getValue().roles()).isEmpty();
		assertThat(saved.getValue().email()).isEqualTo("ana@example.com");
		assertThat(response).isEqualTo(new UserResponse(ID, "Ana", "Pérez", "Calle 10", "+573001234567",
				"ana@example.com", "ana.perez"));
	}

	@Test
	void registerRejectsEmailAlreadyRegistered() {
		given(userQueryRepository.existsByEmail("ana@example.com")).willReturn(true);

		assertThatThrownBy(() -> registerUserCommandHandler.register(request("ANA@example.com")))
			.isInstanceOf(DuplicateUserException.class)
			.hasMessage("Email is already registered");
		verify(userCommandRepository, never()).save(any());
		verify(passwordHasher, never()).hash(anyString());
	}

	@Test
	void registerRejectsUsernameAlreadyRegistered() {
		given(userQueryRepository.existsByUsername("ana.perez")).willReturn(true);

		assertThatThrownBy(() -> registerUserCommandHandler.register(request("ana@example.com")))
			.isInstanceOf(DuplicateUserException.class)
			.hasMessage("Username is already registered");
		verify(userCommandRepository, never()).save(any());
	}

	private static RegisterUserRequest request(String email) {
		return new RegisterUserRequest("Ana", "Pérez", "Calle 10", "+573001234567", email, "ana.perez", "Secret#1");
	}

	private static User withId(User user) {
		return new User(ID, user.firstName(), user.lastName(), user.address(), user.phone(), user.email(),
				user.username(), user.passwordHash(), Set.copyOf(user.roles()));
	}

}
