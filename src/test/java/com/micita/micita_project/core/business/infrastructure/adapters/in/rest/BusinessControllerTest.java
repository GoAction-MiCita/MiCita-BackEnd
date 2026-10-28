package com.micita.micita_project.core.business.infrastructure.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.business.application.dto.request.CreateBusinessRequest;
import com.micita.micita_project.core.business.application.dto.response.BusinessResponse;
import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.business.application.ports.in.CreateBusinessUseCase;
import com.micita.micita_project.core.business.domain.exceptions.BusinessAlreadyExistsException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * HU-03: {@code POST /api/negocios/onboarding} answers 201, 400, 401 and 409, and the owner always
 * comes from the access token.
 */
@WebMvcTest(BusinessController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class BusinessControllerTest {

	private static final UUID USER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final String VALID_BODY = "{\"name\":\"El Corte\",\"taxId\":\"900123456-7\",\"address\":\"Calle 10\","
			+ "\"phone\":\"+573001234567\",\"email\":\"contacto@elcorte.com\"}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateBusinessUseCase createBusinessUseCase;

	@MockitoBean
	private BusinessOwnershipQueryUseCase businessOwnershipQuery;

	// SecurityConfig's AuthenticationManager needs one; the JPA implementation is outside the slice.
	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	void onboardingCreatesBusinessForTheAuthenticatedUser() throws Exception {
		UUID businessId = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");
		given(createBusinessUseCase.create(eq(USER_ID), any())).willReturn(new BusinessResponse(businessId, "El Corte",
				"900123456-7", "Calle 10", "+573001234567", "contacto@elcorte.com", "ACTIVE", "America/Bogota",
				Instant.parse("2026-10-27T15:00:00Z")));

		mockMvc.perform(onboarding(VALID_BODY).with(userJwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(businessId.toString()))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.timeZone").value("America/Bogota"));
		verify(createBusinessUseCase).create(USER_ID, new CreateBusinessRequest("El Corte", "900123456-7", "Calle 10",
				"+573001234567", "contacto@elcorte.com"));
	}

	@Test
	void ownerIdInTheBodyIsIgnored() throws Exception {
		mockMvc.perform(onboarding("{\"ownerId\":\"" + UUID.randomUUID() + "\",\"name\":\"El Corte\","
				+ "\"address\":\"Calle 10\",\"phone\":\"+573001234567\"}").with(userJwt()))
			.andExpect(status().isCreated());
		verify(createBusinessUseCase).create(eq(USER_ID), any());
	}

	@Test
	void blankRequiredFieldsReturnErrorsPerField() throws Exception {
		mockMvc.perform(onboarding("{\"name\":\"\",\"address\":\" \",\"phone\":\"\",\"email\":\"bad\"}").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.address").exists())
			.andExpect(jsonPath("$.errors.phone").exists())
			.andExpect(jsonPath("$.errors.email").exists());
		verify(createBusinessUseCase, never()).create(any(), any());
	}

	@Test
	void unauthenticatedRequestIsUnauthorized() throws Exception {
		mockMvc.perform(onboarding(VALID_BODY)).andExpect(status().isUnauthorized());
		verify(createBusinessUseCase, never()).create(any(), any());
	}

	@Test
	void tokenWithoutUserIdIsUnauthorized() throws Exception {
		mockMvc.perform(onboarding(VALID_BODY).with(jwt()))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Access token has no valid user id: log in again"));
		verify(createBusinessUseCase, never()).create(any(), any());
	}

	@Test
	void userWithABusinessGetsConflict() throws Exception {
		given(createBusinessUseCase.create(eq(USER_ID), any())).willThrow(new BusinessAlreadyExistsException());

		mockMvc.perform(onboarding(VALID_BODY).with(userJwt()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("User already has a business"));
	}

	private static MockHttpServletRequestBuilder onboarding(String body) {
		return post(BusinessController.ONBOARDING_PATH).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
		return jwt().jwt(token -> token.claim(SecurityConfig.USER_ID_CLAIM, USER_ID.toString()));
	}

}
