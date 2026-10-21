package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.query;

import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.models.User;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.mappers.UserBDMapper;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.UserJpaRepository;

import org.springframework.stereotype.Component;

@Component
public class UserQueryJpaAdapter implements UserQueryRepositoryPort {

	private final UserJpaRepository userJpaRepository;

	private final UserBDMapper userBDMapper;

	public UserQueryJpaAdapter(UserJpaRepository userJpaRepository, UserBDMapper userBDMapper) {
		this.userJpaRepository = userJpaRepository;
		this.userBDMapper = userBDMapper;
	}

	@Override
	public boolean existsByEmail(String email) {
		return userJpaRepository.existsByEmail(email);
	}

	@Override
	public boolean existsByUsername(String username) {
		return userJpaRepository.existsByUsername(username);
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return userJpaRepository.findByUsername(username).map(userBDMapper::toDomain);
	}

	@Override
	public Optional<User> findById(UUID id) {
		return userJpaRepository.findById(id).map(userBDMapper::toDomain);
	}

}
