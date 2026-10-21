package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

	@Mock
	private UserQueryRepositoryPort userQueryRepository;

	@InjectMocks
	private JpaUserDetailsService userDetailsService;

	@Test
	void loadUserByUsernamePrefixesRolesAndKeepsAccount() {
		User user = user(Set.of("ADMIN"));
		given(userQueryRepository.findByUsername("admin")).willReturn(Optional.of(user));

		UserDetails details = userDetailsService.loadUserByUsername("admin");

		assertThat(details.getUsername()).isEqualTo("admin");
		assertThat(details.getPassword()).isEqualTo("{bcrypt}hash");
		assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
		assertThat(details).isInstanceOf(AccountUserDetails.class);
		assertThat(((AccountUserDetails) details).account()).isEqualTo(AuthenticatedUser.of(user));
	}

	@Test
	void loadUserByUsernameSupportsUsersWithoutRoles() {
		given(userQueryRepository.findByUsername("admin")).willReturn(Optional.of(user(Set.of())));

		assertThat(userDetailsService.loadUserByUsername("admin").getAuthorities()).isEmpty();
	}

	@Test
	void loadUserByUsernameFailsForUnknownUser() {
		given(userQueryRepository.findByUsername("ghost")).willReturn(Optional.empty());

		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
			.isInstanceOf(UsernameNotFoundException.class);
	}

	private static User user(Set<String> roles) {
		return new User(UUID.randomUUID(), "Admin", "MiCita", "N/A", "0000000", "admin@micita.local", "admin",
				"{bcrypt}hash", roles);
	}

}
