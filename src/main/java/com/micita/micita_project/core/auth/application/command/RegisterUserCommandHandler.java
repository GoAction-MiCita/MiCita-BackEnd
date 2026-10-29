package com.micita.micita_project.core.auth.application.command;

import java.util.Locale;

import com.micita.micita_project.core.auth.application.dto.request.RegisterUserRequest;
import com.micita.micita_project.core.auth.application.dto.response.UserResponse;
import com.micita.micita_project.core.auth.application.mappers.UserMapper;
import com.micita.micita_project.core.auth.application.ports.in.RegisterUserUseCase;
import com.micita.micita_project.core.auth.application.ports.out.PasswordHasherPort;
import com.micita.micita_project.core.auth.application.ports.out.command.UserCommandRepositoryPort;
import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.exceptions.DuplicateUserException;
import com.micita.micita_project.core.auth.domain.models.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUserCommandHandler implements RegisterUserUseCase {

	private final UserQueryRepositoryPort userQueryRepository;

	private final UserCommandRepositoryPort userCommandRepository;

	private final PasswordHasherPort passwordHasher;

	private final UserMapper userMapper;

	@Override
	public UserResponse register(RegisterUserRequest request) {
		// Emails are unique regardless of case.
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (userQueryRepository.existsByEmail(email)) {
			throw DuplicateUserException.email();
		}
		if (userQueryRepository.existsByUsername(request.username())) {
			throw DuplicateUserException.username();
		}
		User user = userMapper.toNewUser(request, email, passwordHasher.hash(request.password()));
		return userMapper.toResponse(userCommandRepository.save(user));
	}

}
