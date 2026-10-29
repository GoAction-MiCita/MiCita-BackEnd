package com.micita.micita_project.core.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.micita.micita_project.TestcontainersConfiguration;
import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.UserJpaRepository;
import com.micita.micita_project.core.business.domain.models.BusinessStatus;
import com.micita.micita_project.core.business.infrastructure.adapters.in.rest.BusinessProbeController;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.entities.BusinessEntity;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories.BusinessJpaRepository;
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
 * End-to-end HU-03 and HU-04 against a real PostgreSQL: a new user is held at onboarding, registers
 * a business and then gets in, and login/refresh report {@code hasBusiness=true}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class BusinessOnboardingIntegrationTest {

	private static final String BUSINESS = "{\"name\":\"El Corte\",\"taxId\":\"900123456-7\",\"address\":\"Calle 10\","
			+ "\"phone\":\"+573001234567\",\"email\":\"Contacto@ElCorte.com\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BusinessJpaRepository businessJpaRepository;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void newUserIsHeldAtOnboardingUntilTheBusinessExists() throws Exception {
		String username = newUser();
		String loginBody = login(username);
		String access = JsonPath.read(loginBody, "$.accessToken");
		String refresh = JsonPath.read(loginBody, "$.refreshToken");
		assertThat(hasBusiness(access)).isFalse();

		probe(access).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));

		onboard(access, BUSINESS).andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.name").value("El Corte"))
			.andExpect(jsonPath("$.email").value("contacto@elcorte.com"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.timeZone").value("America/Bogota"));

		UUID userId = userJpaRepository.findByUsername(username).orElseThrow().getId();
		BusinessEntity stored = businessJpaRepository.findAll()
			.stream()
			.filter(business -> business.getOwnerId().equals(userId))
			.findFirst()
			.orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(BusinessStatus.ACTIVE);
		assertThat(stored.getTimeZone()).isEqualTo("America/Bogota");

		// Ownership is checked in PostgreSQL, so the old token already gets through.
		probe(access).andExpect(status().isOk());

		String refreshed = mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + refresh + "\"}"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(hasBusiness(JsonPath.read(refreshed, "$.accessToken"))).isTrue();
		assertThat(hasBusiness(JsonPath.read(login(username), "$.accessToken"))).isTrue();
	}

	@Test
	void secondBusinessForTheSameUserIsConflict() throws Exception {
		String access = JsonPath.read(login(newUser()), "$.accessToken");

		onboard(access, BUSINESS).andExpect(status().isCreated());
		onboard(access, BUSINESS).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("User already has a business"));
	}

	@Test
	void invalidBusinessIsBadRequestAndNothingIsStored() throws Exception {
		String access = JsonPath.read(login(newUser()), "$.accessToken");
		long before = businessJpaRepository.count();

		onboard(access, "{\"name\":\"\",\"address\":\"Calle 10\",\"phone\":\"abc\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.phone").exists());
		assertThat(businessJpaRepository.count()).isEqualTo(before);
	}

	@Test
	void onboardingWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/business/onboarding").contentType(MediaType.APPLICATION_JSON).content(BUSINESS))
			.andExpect(status().isUnauthorized());
	}

	private String newUser() throws Exception {
		String username = "owner-" + UUID.randomUUID().toString().substring(0, 8);
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Ana\",\"lastName\":\"Pérez\",\"address\":\"Calle 10\",\"phone\":\"+573001234567\","
					+ "\"email\":\"" + username + "@example.com\",\"username\":\"" + username
					+ "\",\"password\":\"Secret#1\"}"))
			.andExpect(status().isCreated());
		return username;
	}

	private String login(String username) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"" + username + "\",\"password\":\"Secret#1\"}"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private ResultActions onboard(String accessToken, String body) throws Exception {
		return mockMvc.perform(post("/api/business/onboarding").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions probe(String accessToken) throws Exception {
		return mockMvc.perform(get(BusinessProbeController.PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
	}

	private boolean hasBusiness(String accessToken) {
		return jwtDecoder.decode(accessToken).getClaimAsBoolean(SecurityConfig.HAS_BUSINESS_CLAIM);
	}

}
