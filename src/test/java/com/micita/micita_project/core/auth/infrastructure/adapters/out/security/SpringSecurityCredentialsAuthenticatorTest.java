package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.auth.domain.exceptions.InvalidCredentialsException;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class SpringSecurityCredentialsAuthenticatorTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@InjectMocks
	private SpringSecurityCredentialsAuthenticator authenticator;

	@Test
	void authenticateReturnsTheAccountOfTheAuthenticatedPrincipal() {
		UUID id = UUID.randomUUID();
		AccountUserDetails principal = new AccountUserDetails(new User(id, "Ana", "Pérez", "Calle 10",
				"+573001234567", "ana@example.com", "ana", "{bcrypt}hash", Set.of("ADMIN")));
		given(authenticationManager.authenticate(any())).willReturn(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

		AuthenticatedUser user = authenticator.authenticate("ana", "secret");

		assertThat(user).isEqualTo(new AuthenticatedUser(id, "ana", List.of("ADMIN")));
		ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
		verify(authenticationManager).authenticate(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("ana");
		assertThat(captor.getValue().getCredentials()).isEqualTo("secret");
	}

	@Test
	void authenticateTranslatesSpringFailureToDomainException() {
		given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("bad"));

		assertThatThrownBy(() -> authenticator.authenticate("ana", "wrong"))
			.isInstanceOf(InvalidCredentialsException.class);
	}

}
