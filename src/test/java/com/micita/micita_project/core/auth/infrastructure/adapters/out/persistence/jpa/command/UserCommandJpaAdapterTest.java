package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.auth.domain.exceptions.DuplicateUserException;
import com.micita.micita_project.core.auth.domain.models.User;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.entities.UserEntity;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.mappers.UserBDMapper;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserCommandJpaAdapterTest {

	private static final UUID ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	@Mock
	private UserJpaRepository userJpaRepository;

	@Spy
	private UserBDMapper userBDMapper = Mappers.getMapper(UserBDMapper.class);

	@InjectMocks
	private UserCommandJpaAdapter adapter;

	@Test
	void saveMapsBothWaysAndReturnsGeneratedId() {
		given(userJpaRepository.saveAndFlush(any())).willAnswer(invocation -> {
			UserEntity entity = invocation.getArgument(0);
			entity.setId(ID);
			return entity;
		});

		User saved = adapter.save(user());

		assertThat(saved).isEqualTo(new User(ID, "Ana", "Pérez", "Calle 10", "+573001234567", "ana@example.com",
				"ana.perez", "{bcrypt}hash", Set.of("ADMIN")));
	}

	@Test
	void saveTranslatesUniqueViolationToDuplicateUser() {
		given(userJpaRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> adapter.save(user())).isInstanceOf(DuplicateUserException.class)
			.hasMessage("Email or username is already registered");
	}

	private static User user() {
		return new User(null, "Ana", "Pérez", "Calle 10", "+573001234567", "ana@example.com", "ana.perez",
				"{bcrypt}hash", Set.of("ADMIN"));
	}

}
