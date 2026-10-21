package com.micita.micita_project.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.micita.micita_project.TestcontainersConfiguration;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.entities.UserEntity;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.repositories.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end HU-01 flow against a real PostgreSQL: register, then log in with the new account.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class UserRegistrationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserJpaRepository userJpaRepository;

	@Test
	void registeredUserIsStoredHashedWithoutRolesAndCanLogIn() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("carla@example.com", "carla.gomez")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("carla@example.com"));

		UserEntity stored = userJpaRepository.findByUsername("carla.gomez").orElseThrow();
		assertThat(stored.getPasswordHash()).startsWith("{bcrypt}").isNotEqualTo("Secret#1");
		assertThat(stored.getRoles()).isEmpty();

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"carla.gomez\",\"password\":\"Secret#1\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void duplicateEmailOrUsernameReturnsConflict() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("dario@example.com", "dario")))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("DARIO@example.com", "dario2")))
			.andExpect(status().isConflict());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("other@example.com", "dario")))
			.andExpect(status().isConflict());
	}

	@Test
	void invalidPayloadReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("not-an-email", "elena")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.email").exists());
	}

	@Test
	void bootstrapAdminIsSeededAndCanLogIn() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"test-user\",\"password\":\"test-password\"}"))
			.andExpect(status().isOk());
		assertThat(userJpaRepository.findByUsername("test-user").orElseThrow().getRoles()).containsExactly("ADMIN");
	}

	private static String registerBody(String email, String username) {
		return "{\"firstName\":\"Ana\",\"lastName\":\"Pérez\",\"address\":\"Calle 10\",\"phone\":\"+573001234567\","
				+ "\"email\":\"" + email + "\",\"username\":\"" + username + "\",\"password\":\"Secret#1\"}";
	}

}
