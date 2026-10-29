package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.command;

import com.micita.micita_project.core.auth.application.ports.out.command.UserCommandRepositoryPort;
import com.micita.micita_project.core.auth.domain.exceptions.DuplicateUserException;
import com.micita.micita_project.core.auth.domain.models.User;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.mappers.UserBDMapper;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCommandJpaAdapter implements UserCommandRepositoryPort {

	private final UserJpaRepository userJpaRepository;

	private final UserBDMapper userBDMapper;

	@Override
	public User save(User user) {
		try {
			// Flush so a unique-constraint violation surfaces here, not at commit time.
			return userBDMapper.toDomain(userJpaRepository.saveAndFlush(userBDMapper.toEntity(user)));
		}
		catch (DataIntegrityViolationException ex) {
			// A concurrent registration took the email or username after the existence checks.
			throw DuplicateUserException.emailOrUsername();
		}
	}

}
