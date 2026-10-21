package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class SecureRandomRefreshTokenGeneratorTest {

	private final SecureRandomRefreshTokenGenerator generator = new SecureRandomRefreshTokenGenerator();

	@Test
	void generatedTokensAreUrlSafe256BitRandomValues() {
		String token = generator.generate();

		assertThat(token).matches("[A-Za-z0-9_-]{43}");
		assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
		assertThat(generator.generate()).isNotEqualTo(token);
	}

	@Test
	void hashIsDeterministicSha256HexAndNeverTheRawValue() {
		String token = generator.generate();

		assertThat(generator.hash(token)).isEqualTo(generator.hash(token)).hasSize(64).doesNotContain(token);
		// SHA-256("abc"), the FIPS 180-2 test vector.
		assertThat(generator.hash("abc"))
			.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

}
