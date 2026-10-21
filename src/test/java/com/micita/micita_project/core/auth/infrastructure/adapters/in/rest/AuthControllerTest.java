package com.micita.micita_project.core.auth.infrastructure.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.auth.application.dto.request.LoginRequest;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.application.dto.response.UserResponse;
import com.micita.micita_project.core.auth.application.dto.request.RefreshTokenRequest;
import com.micita.micita_project.core.auth.application.ports.in.LoginUseCase;
import com.micita.micita_project.core.auth.application.ports.in.LogoutUseCase;
import com.micita.micita_project.core.auth.application.ports.in.RefreshTokenUseCase;
import com.micita.micita_project.core.auth.application.ports.in.RegisterUserUseCase;
import com.micita.micita_project.core.auth.domain.exceptions.DuplicateUserException;
import com.micita.micita_project.core.auth.domain.exceptions.InvalidCredentialsException;
import com.micita.micita_project.core.auth.domain.exceptions.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

	private static final TokenResponse TOKENS = new TokenResponse("jwt-token", "Bearer", 900, "refresh-token", 604800);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LoginUseCase loginUseCase;

	@MockitoBean
	private RegisterUserUseCase registerUserUseCase;

	@MockitoBean
	private RefreshTokenUseCase refreshTokenUseCase;

	@MockitoBean
	private LogoutUseCase logoutUseCase;

	// SecurityConfig's AuthenticationManager needs one; the JPA implementation is outside the slice.
	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	void registerReturnsCreatedUserWithoutPassword() throws Exception {
		UUID id = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");
		given(registerUserUseCase.register(any())).willReturn(new UserResponse(id, "Ana", "Pérez", "Calle 10",
				"+573001234567", "ana@example.com", "ana.perez"));

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("Secret#1")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(id.toString()))
			.andExpect(jsonPath("$.username").value("ana.perez"))
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void registerWithBlankFieldsReturnsErrorsPerField() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"\",\"lastName\":\"\",\"address\":\"\",\"phone\":\"\",\"email\":\"\","
					+ "\"username\":\"\",\"password\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.firstName").exists())
			.andExpect(jsonPath("$.errors.lastName").exists())
			.andExpect(jsonPath("$.errors.address").exists())
			.andExpect(jsonPath("$.errors.phone").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.username").exists())
			.andExpect(jsonPath("$.errors.password").exists());
		verify(registerUserUseCase, never()).register(any());
	}

	@Test
	void registerWithWeakPasswordReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("secret1")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void registerWithDuplicateEmailReturnsConflictProblem() throws Exception {
		given(registerUserUseCase.register(any())).willThrow(DuplicateUserException.email());

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("Secret#1")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("Email is already registered"));
	}

	@Test
	void loginReturnsAccessAndRefreshTokens() throws Exception {
		given(loginUseCase.login(new LoginRequest("ana", "secret"))).willReturn(TOKENS);

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"ana\",\"password\":\"secret\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("jwt-token"))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
			.andExpect(jsonPath("$.refreshExpiresIn").value(604800));
	}

	@Test
	void loginWithBlankFieldsReturnsValidationProblem() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"\",\"password\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.detail").value("Request validation failed"))
			.andExpect(jsonPath("$.errors.username").exists())
			.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void loginWithInvalidCredentialsReturnsUnauthorizedProblem() throws Exception {
		given(loginUseCase.login(any())).willThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\":\"ana\",\"password\":\"wrong\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.detail").value("Invalid credentials"));
	}

	@Test
	void refreshReturnsNewTokens() throws Exception {
		given(refreshTokenUseCase.refresh(new RefreshTokenRequest("refresh-token"))).willReturn(TOKENS);

		mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"refresh-token\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value("jwt-token"))
			.andExpect(jsonPath("$.refreshToken").value("refresh-token"));
	}

	@Test
	void refreshWithInvalidTokenReturnsUnauthorizedProblem() throws Exception {
		given(refreshTokenUseCase.refresh(any())).willThrow(new InvalidRefreshTokenException());

		mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"revoked\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
	}

	@Test
	void refreshWithBlankTokenReturnsValidationProblem() throws Exception {
		mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.refreshToken").exists());
		verify(refreshTokenUseCase, never()).refresh(any());
	}

	@Test
	void logoutRevokesRefreshTokenWithoutAccessToken() throws Exception {
		mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"refresh-token\"}"))
			.andExpect(status().isNoContent());
		verify(logoutUseCase).logout(new RefreshTokenRequest("refresh-token"));
	}

	@Test
	void logoutWithBlankTokenReturnsValidationProblem() throws Exception {
		mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\" \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.refreshToken").exists());
		verify(logoutUseCase, never()).logout(any());
	}

	@Test
	void privateEndpointWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/servicios")).andExpect(status().isUnauthorized());
	}

	private static String registerBody(String password) {
		return "{\"firstName\":\"Ana\",\"lastName\":\"Pérez\",\"address\":\"Calle 10\",\"phone\":\"+573001234567\","
				+ "\"email\":\"ana@example.com\",\"username\":\"ana.perez\",\"password\":\"" + password + "\"}";
	}

}
