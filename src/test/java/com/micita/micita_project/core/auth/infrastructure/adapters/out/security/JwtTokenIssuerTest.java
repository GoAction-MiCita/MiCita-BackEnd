package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.micita.micita_project.common.shared.security.JwtProperties;
import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.IssuedToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtTokenIssuerTest {

	private static final String SECRET = "unit-test-secret-0123456789abcdef0123456789";

	private final SecurityConfig securityConfig = new SecurityConfig();

	private final JwtProperties properties = new JwtProperties(SECRET, "micita", Duration.ofMinutes(15),
			Duration.ofDays(7));

	private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "ana", List.of("ADMIN"));

	private JwtTokenIssuer tokenIssuer;

	private JwtDecoder jwtDecoder;

	@BeforeEach
	void setUp() {
		tokenIssuer = new JwtTokenIssuer(securityConfig.jwtEncoder(properties), properties);
		jwtDecoder = securityConfig.jwtDecoder(properties);
	}

	@Test
	void issuedTokenCarriesSubjectPrefixedRolesIssuerAndExpiration() {
		IssuedToken token = tokenIssuer.issue(user, false);

		Jwt jwt = jwtDecoder.decode(token.value());
		assertThat(jwt.getSubject()).isEqualTo("ana");
		assertThat(jwt.getClaimAsStringList(SecurityConfig.ROLES_CLAIM)).containsExactly("ROLE_ADMIN");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("micita");
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
		assertThat(token.expiresInSeconds()).isEqualTo(900);
	}

	@Test
	void issuedTokenCarriesHasBusinessFlag() {
		assertThat(jwtDecoder.decode(tokenIssuer.issue(user, false).value())
			.getClaimAsBoolean(SecurityConfig.HAS_BUSINESS_CLAIM)).isFalse();
		assertThat(jwtDecoder.decode(tokenIssuer.issue(user, true).value())
			.getClaimAsBoolean(SecurityConfig.HAS_BUSINESS_CLAIM)).isTrue();
	}

	@Test
	void expiredTokenIsRejectedByDecoder() {
		Clock pastClock = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
		JwtTokenIssuer pastIssuer = new JwtTokenIssuer(securityConfig.jwtEncoder(properties), properties, pastClock);
		String token = pastIssuer.issue(user, false).value();

		assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtValidationException.class);
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejectedByDecoder() {
		JwtProperties otherProperties = new JwtProperties("another-secret-0123456789abcdef0123456789", "micita",
				Duration.ofMinutes(15), Duration.ofDays(7));
		String token = new JwtTokenIssuer(securityConfig.jwtEncoder(otherProperties), otherProperties)
			.issue(user, false)
			.value();

		assertThatThrownBy(() -> jwtDecoder.decode(token))
			.isInstanceOf(org.springframework.security.oauth2.jwt.BadJwtException.class);
	}

	@Test
	void propertiesRejectShortSecret() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new JwtProperties("too-short", "micita", Duration.ofMinutes(15), Duration.ofDays(7)));
	}

	@Test
	void propertiesRejectBlankIssuer() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JwtProperties(SECRET, " ", Duration.ofMinutes(15), Duration.ofDays(7)));
	}

	@Test
	void propertiesRejectNonPositiveExpirations() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JwtProperties(SECRET, "micita", Duration.ZERO, Duration.ofDays(7)))
			.withMessageContaining("micita.security.jwt.expiration");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new JwtProperties(SECRET, "micita", Duration.ofMinutes(15), null))
			.withMessageContaining("micita.security.jwt.refresh-expiration");
	}

}
