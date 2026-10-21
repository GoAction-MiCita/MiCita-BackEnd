package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

@ExtendWith(MockitoExtension.class)
class UserQueryJpaAdapterTest {

	@Mock
	private UserJpaRepository userJpaRepository;

	@Spy
	private UserBDMapper userBDMapper = Mappers.getMapper(UserBDMapper.class);

	@InjectMocks
	private UserQueryJpaAdapter adapter;

	@Test
	void existsChecksDelegateToRepository() {
		given(userJpaRepository.existsByEmail("ana@example.com")).willReturn(true);
		given(userJpaRepository.existsByUsername("ana.perez")).willReturn(false);

		assertThat(adapter.existsByEmail("ana@example.com")).isTrue();
		assertThat(adapter.existsByUsername("ana.perez")).isFalse();
	}

	@Test
	void findByUsernameMapsEntityToDomain() {
		UUID id = UUID.randomUUID();
		UserEntity entity = new UserEntity();
		entity.setId(id);
		entity.setFirstName("Ana");
		entity.setLastName("Pérez");
		entity.setAddress("Calle 10");
		entity.setPhone("+573001234567");
		entity.setEmail("ana@example.com");
		entity.setUsername("ana.perez");
		entity.setPasswordHash("{bcrypt}hash");
		entity.setRoles(Set.of());
		given(userJpaRepository.findByUsername("ana.perez")).willReturn(Optional.of(entity));

		assertThat(adapter.findByUsername("ana.perez")).contains(new User(id, "Ana", "Pérez", "Calle 10",
				"+573001234567", "ana@example.com", "ana.perez", "{bcrypt}hash", Set.of()));
	}

	@Test
	void findByIdMapsEntityToDomain() {
		UUID id = UUID.randomUUID();
		UserEntity entity = new UserEntity();
		entity.setId(id);
		entity.setUsername("ana.perez");
		entity.setRoles(Set.of("ADMIN"));
		given(userJpaRepository.findById(id)).willReturn(Optional.of(entity));

		assertThat(adapter.findById(id)).hasValueSatisfying(user -> {
			assertThat(user.id()).isEqualTo(id);
			assertThat(user.username()).isEqualTo("ana.perez");
			assertThat(user.roles()).containsExactly("ADMIN");
		});
	}

	@Test
	void findByUsernameReturnsEmptyWhenMissing() {
		given(userJpaRepository.findByUsername("ghost")).willReturn(Optional.empty());

		assertThat(adapter.findByUsername("ghost")).isEmpty();
	}

}
