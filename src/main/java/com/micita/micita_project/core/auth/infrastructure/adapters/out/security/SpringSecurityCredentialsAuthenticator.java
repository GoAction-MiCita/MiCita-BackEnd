package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import com.micita.micita_project.core.auth.application.ports.out.CredentialsAuthenticatorPort;
import com.micita.micita_project.core.auth.domain.exceptions.InvalidCredentialsException;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCredentialsAuthenticator implements CredentialsAuthenticatorPort {

	private final AuthenticationManager authenticationManager;

	public SpringSecurityCredentialsAuthenticator(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	@Override
	public AuthenticatedUser authenticate(String username, String password) {
		Authentication authentication;
		try {
			authentication = authenticationManager
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
		}
		catch (AuthenticationException ex) {
			throw new InvalidCredentialsException();
		}
		// JpaUserDetailsService always produces AccountUserDetails.
		return ((AccountUserDetails) authentication.getPrincipal()).account();
	}

}
