package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import com.micita.micita_project.core.auth.application.ports.out.PasswordHasherPort;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashes with the application {@link PasswordEncoder} (delegating, BCrypt by default), so stored
 * hashes carry an algorithm prefix such as {@code {bcrypt}}.
 */
@Component
@RequiredArgsConstructor
public class SpringPasswordHasher implements PasswordHasherPort {

	private final PasswordEncoder passwordEncoder;

	@Override
	public String hash(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

}
