package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import com.micita.micita_project.core.auth.application.ports.out.RefreshTokenGeneratorPort;

import org.springframework.stereotype.Component;

/**
 * Refresh tokens are 256 random bits, URL-safe Base64 encoded. They are stored as SHA-256: with
 * that much entropy a slow, salted hash (BCrypt) adds nothing, and a deterministic hash lets the
 * token be looked up directly.
 */
@Component
public class SecureRandomRefreshTokenGenerator implements RefreshTokenGeneratorPort {

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public String generate() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	@Override
	public String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			// Every JVM is required to provide SHA-256.
			throw new IllegalStateException(ex);
		}
	}

}
