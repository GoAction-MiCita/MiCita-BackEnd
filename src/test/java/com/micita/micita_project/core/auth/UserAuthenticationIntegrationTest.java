package com.micita.micita_project.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.micita.micita_project.TestcontainersConfiguration;
import com.micita.micita_project.common.shared.security.AdminProbeController;
import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.auth.application.ports.out.RefreshTokenGeneratorPort;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.entities.RefreshTokenEntity;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.RefreshTokenJpaRepository;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.UserJpaRepository;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories.BusinessJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end HU-02 flow against a real PostgreSQL: login, refresh with rotation, logout and access
 * control on private resources.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class UserAuthenticationIntegrationTest {

	private static final String ADMIN_CREDENTIALS = "{\"username\":\"test-user\",\"password\":\"test-password\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RefreshTokenJpaRepository refreshTokenJpaRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private RefreshTokenGeneratorPort refreshTokenGenerator;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private BusinessJpaRepository businessJpaRepository;

	// The bootstrap admin starts without a business in every test; the ones that reach a private
	// resource onboard it first (HU-04).
	@BeforeEach
	void removeBusinesses() {
		businessJpaRepository.deleteAll();
	}

	@Test
	void loginReturnsTokensWithHasBusinessAndStoresOnlyTheRefreshHash() throws Exception {
		String body = login(ADMIN_CREDENTIALS).andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andExpect(jsonPath("$.refreshExpiresIn").value(604800))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String accessToken = JsonPath.read(body, "$.accessToken");
		String refreshToken = JsonPath.read(body, "$.refreshToken");

		assertThat(jwtDecoder.decode(accessToken).getClaimAsBoolean(SecurityConfig.HAS_BUSINESS_CLAIM)).isFalse();
		RefreshTokenEntity stored = refreshTokenJpaRepository.findByTokenHash(refreshTokenGenerator.hash(refreshToken))
			.orElseThrow();
		assertThat(stored.getTokenHash()).isNotEqualTo(refreshToken);
		assertThat(stored.getUserId()).isEqualTo(userJpaRepository.findByUsername("test-user").orElseThrow().getId());
		assertThat(stored.getRevokedAt()).isNull();
		assertThat(refreshTokenJpaRepository.findAll()).extracting(RefreshTokenEntity::getTokenHash)
			.doesNotContain(refreshToken);
	}

	@Test
	void loginWithWrongPasswordIsUnauthorized() throws Exception {
		login("{\"username\":\"test-user\",\"password\":\"wrong\"}").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void refreshIssuesNewTokensAndTheOldRefreshTokenStopsWorking() throws Exception {
		String oldRefresh = JsonPath.read(loginBody(), "$.refreshToken");

		String refreshed = refresh(oldRefresh).andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String newAccess = JsonPath.read(refreshed, "$.accessToken");
		String newRefresh = JsonPath.read(refreshed, "$.refreshToken");

		assertThat(newRefresh).isNotEqualTo(oldRefresh);
		onboard(newAccess);
		mockMvc.perform(get(AdminProbeController.PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccess))
			.andExpect(status().isOk());
		refresh(oldRefresh).andExpect(status().isUnauthorized());
		refresh(newRefresh).andExpect(status().isOk());
	}

	@Test
	void unknownRefreshTokenIsUnauthorized() throws Exception {
		refresh("not-a-real-token").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
	}

	@Test
	void logoutRevokesTheRefreshToken() throws Exception {
		String refreshToken = JsonPath.read(loginBody(), "$.refreshToken");

		mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + refreshToken + "\"}"))
			.andExpect(status().isNoContent());

		assertThat(refreshTokenJpaRepository.findByTokenHash(refreshTokenGenerator.hash(refreshToken)).orElseThrow()
			.getRevokedAt()).isNotNull();
		refresh(refreshToken).andExpect(status().isUnauthorized());
	}

	@Test
	void privateResourceRequiresValidTokenAndRole() throws Exception {
		mockMvc.perform(get(AdminProbeController.PATH)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(AdminProbeController.PATH).header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
			.andExpect(status().isUnauthorized());

		String adminAccess = JsonPath.read(loginBody(), "$.accessToken");
		onboard(adminAccess);
		mockMvc.perform(get(AdminProbeController.PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccess))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Ana\",\"lastName\":\"Pérez\",\"address\":\"Calle 10\",\"phone\":\"+573001234567\","
					+ "\"email\":\"owner@example.com\",\"username\":\"owner\",\"password\":\"Secret#1\"}"))
			.andExpect(status().isCreated());
		String ownerAccess = JsonPath.read(login("{\"username\":\"owner\",\"password\":\"Secret#1\"}")
			.andReturn().getResponse().getContentAsString(), "$.accessToken");
		mockMvc.perform(get(AdminProbeController.PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAccess))
			.andExpect(status().isForbidden());
	}

	private ResultActions login(String credentials) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials));
	}

	private String loginBody() throws Exception {
		return login(ADMIN_CREDENTIALS).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	private void onboard(String accessToken) throws Exception {
		mockMvc.perform(post("/api/business/onboarding").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Admin Shop\",\"address\":\"Calle 1\",\"phone\":\"+573001112233\"}"))
			.andExpect(status().isCreated());
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + refreshToken + "\"}"));
	}

}
