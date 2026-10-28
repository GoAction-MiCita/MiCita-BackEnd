package com.micita.micita_project.core.auth.infrastructure.adapters.out.security;

import java.time.Clock;
import java.time.Instant;

import com.micita.micita_project.common.shared.security.JwtProperties;
import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.auth.application.ports.out.TokenIssuerPort;
import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.domain.models.IssuedToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer implements TokenIssuerPort {

	private final JwtEncoder jwtEncoder;

	private final JwtProperties jwtProperties;

	private final Clock clock;

	@Autowired
	public JwtTokenIssuer(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
		this(jwtEncoder, jwtProperties, Clock.systemUTC());
	}

	JwtTokenIssuer(JwtEncoder jwtEncoder, JwtProperties jwtProperties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
		this.clock = clock;
	}

	@Override
	public IssuedToken issue(AuthenticatedUser user, boolean hasBusiness) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(jwtProperties.issuer())
			.issuedAt(now)
			.expiresAt(now.plus(jwtProperties.expiration()))
			.subject(user.username())
			.claim(SecurityConfig.USER_ID_CLAIM, user.userId().toString())
			.claim(SecurityConfig.ROLES_CLAIM, user.roles().stream().map(SecurityConfig.ROLE_PREFIX::concat).toList())
			.claim(SecurityConfig.HAS_BUSINESS_CLAIM, hasBusiness)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(token, jwtProperties.expiration().toSeconds());
	}

}
