package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.User;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Spring Security principal for a stored user. Besides the credentials it keeps the account data
 * (id and bare role names) that login needs to issue tokens.
 */
public class AccountUserDetails extends org.springframework.security.core.userdetails.User {

	private final AuthenticatedUser account;

	public AccountUserDetails(User user) {
		super(user.username(), user.passwordHash(), user.roles()
			.stream()
			.map(role -> new SimpleGrantedAuthority(SecurityConfig.ROLE_PREFIX + role))
			.toList());
		this.account = AuthenticatedUser.of(user);
	}

	public AuthenticatedUser account() {
		return account;
	}

}
