package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class SpringPasswordHasherTest {

	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	private final SpringPasswordHasher hasher = new SpringPasswordHasher(passwordEncoder);

	@Test
	void hashUsesBcryptAndNeverReturnsRawPassword() {
		String hash = hasher.hash("Secret#1");

		assertThat(hash).startsWith("{bcrypt}").doesNotContain("Secret#1");
		assertThat(passwordEncoder.matches("Secret#1", hash)).isTrue();
	}

}
