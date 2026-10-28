package com.micita.micita_project.common.shared.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.micita.micita_project.core.auth.domain.models.AuthenticatedUser;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.security.JwtTokenIssuer;
import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HU-02: a private resource without a valid token is 401; with a valid token but without the
 * required role it is 403.
 */
@WebMvcTest(AdminProbeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SecurityConfigAccessTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtProperties jwtProperties;

	// SecurityConfig's AuthenticationManager needs one; the JPA implementation is outside the slice.
	@MockitoBean
	private UserDetailsService userDetailsService;

	// Everyone owns a business here, so only authentication and roles are under test (HU-04 has its own).
	@MockitoBean
	private BusinessOwnershipQueryUseCase businessOwnershipQuery;

	@BeforeEach
	void everyoneOwnsABusiness() {
		given(businessOwnershipQuery.ownsBusiness(any())).willReturn(true);
	}

	@Test
	void privateResourceWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get(AdminProbeController.PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().string(""));
	}

	@Test
	void privateResourceWithMalformedTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get(AdminProbeController.PATH).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void privateResourceWithoutRequiredRoleIsForbidden() throws Exception {
		mockMvc.perform(get(AdminProbeController.PATH).with(jwt().jwt(token -> token.claim(SecurityConfig.USER_ID_CLAIM,
				UUID.randomUUID().toString()))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void privateResourceWithRequiredRoleIsAllowed() throws Exception {
		mockMvc.perform(get(AdminProbeController.PATH).with(jwt()
			.jwt(token -> token.claim(SecurityConfig.USER_ID_CLAIM, UUID.randomUUID().toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk());
	}

	@Test
	void issuedTokensAreValidatedAndTheirRolesEnforced() throws Exception {
		JwtTokenIssuer tokenIssuer = new JwtTokenIssuer(jwtEncoder, jwtProperties);
		String adminToken = tokenIssuer.issue(new AuthenticatedUser(UUID.randomUUID(), "admin", List.of("ADMIN")), true)
			.value();
		String ownerToken = tokenIssuer.issue(new AuthenticatedUser(UUID.randomUUID(), "ana", List.of()), false)
			.value();

		mockMvc.perform(get(AdminProbeController.PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.andExpect(status().isOk());
		mockMvc.perform(get(AdminProbeController.PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
			.andExpect(status().isForbidden());
	}

}
