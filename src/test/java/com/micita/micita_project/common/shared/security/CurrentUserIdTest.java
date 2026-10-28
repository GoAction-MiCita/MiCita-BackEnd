package com.micita.micita_project.common.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserIdTest {

	@Test
	void readsUserIdFromUidClaim() {
		UUID userId = UUID.randomUUID();

		assertThat(CurrentUserId.from(jwt(userId.toString()))).isEqualTo(userId);
	}

	@Test
	void tokenWithoutUidIsUnauthorized() {
		Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("ana").issuedAt(Instant.now()).build();

		assertThatThrownBy(() -> CurrentUserId.from(jwt)).isInstanceOf(UnauthorizedException.class)
			.hasMessage("Access token has no valid user id: log in again");
	}

	@Test
	void malformedUidIsUnauthorized() {
		assertThatThrownBy(() -> CurrentUserId.from(jwt("not-a-uuid"))).isInstanceOf(UnauthorizedException.class);
	}

	private static Jwt jwt(String userId) {
		return Jwt.withTokenValue("token").header("alg", "HS256").claim(SecurityConfig.USER_ID_CLAIM, userId).build();
	}

}
