package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import com.micita.micita_project.core.auth.application.ports.out.query.UserQueryRepositoryPort;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Loads login credentials from the users table. Roles get the {@code ROLE_} prefix in
 * {@link AccountUserDetails}.
 */
@Component
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

	private final UserQueryRepositoryPort userQueryRepository;

	@Override
	public UserDetails loadUserByUsername(String username) {
		return userQueryRepository.findByUsername(username)
			.map(AccountUserDetails::new)
			.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

}
