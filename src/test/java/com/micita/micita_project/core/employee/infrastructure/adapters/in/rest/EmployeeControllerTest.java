package com.micita.micita_project.core.employee.infrastructure.adapters.in.rest;

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
import java.util.List;
import java.util.UUID;

import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.employee.application.dto.request.CreateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.ports.in.CreateEmployeeUseCase;
import com.micita.micita_project.core.employee.domain.exceptions.DuplicateEmployeeException;
import com.micita.micita_project.core.service.domain.exceptions.InvalidServiceIdsException;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * HU-17: {@code POST /api/empleados} answers 201, 400, 401, 403 and 409, and the business always
 * comes from the access token.
 */
@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class EmployeeControllerTest {

	private static final UUID USER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID CUT = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final String VALID_BODY = "{\"firstName\":\"Carlos\",\"lastName\":\"Gómez\","
			+ "\"phone\":\"+573001234567\",\"email\":\"carlos@elcorte.com\",\"status\":\"ACTIVE\","
			+ "\"serviceIds\":[\"" + CUT + "\"]}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateEmployeeUseCase createEmployeeUseCase;

	@MockitoBean
	private BusinessOwnershipQueryUseCase businessOwnershipQuery;

	// SecurityConfig's AuthenticationManager needs one; the JPA implementation is outside the slice.
	@MockitoBean
	private UserDetailsService userDetailsService;

	@BeforeEach
	void userHasABusiness() {
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(true);
	}

	@Test
	void createsEmployeeForTheCallersBusiness() throws Exception {
		given(createEmployeeUseCase.create(eq(USER_ID), any())).willReturn(new EmployeeResponse(EMPLOYEE_ID, "Carlos",
				"Gómez", "+573001234567", "carlos@elcorte.com", "ACTIVE", List.of(CUT),
				Instant.parse("2026-10-29T15:00:00Z")));

		mockMvc.perform(create(VALID_BODY).with(userJwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.serviceIds[0]").value(CUT.toString()));
		verify(createEmployeeUseCase).create(USER_ID, new CreateEmployeeRequest("Carlos", "Gómez", "+573001234567",
				"carlos@elcorte.com", "ACTIVE", List.of(CUT)));
	}

	@Test
	void businessIdInTheBodyIsIgnored() throws Exception {
		mockMvc.perform(create("{\"businessId\":\"" + UUID.randomUUID() + "\",\"firstName\":\"Carlos\","
				+ "\"lastName\":\"Gómez\",\"status\":\"ACTIVE\"}").with(userJwt()))
			.andExpect(status().isCreated());
		verify(createEmployeeUseCase).create(USER_ID,
				new CreateEmployeeRequest("Carlos", "Gómez", null, null, "ACTIVE", null));
	}

	@Test
	void invalidFieldsReturnErrorsPerField() throws Exception {
		mockMvc.perform(create("{\"firstName\":\"\",\"lastName\":\"" + "x".repeat(101) + "\",\"phone\":\"12-34\","
				+ "\"email\":\"bad\",\"status\":\"DELETED\"}").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.firstName").exists())
			.andExpect(jsonPath("$.errors.lastName").exists())
			.andExpect(jsonPath("$.errors.phone").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.status").value("must be ACTIVE or INACTIVE"));
		verify(createEmployeeUseCase, never()).create(any(), any());
	}

	@Test
	void missingStatusIsBadRequest() throws Exception {
		mockMvc.perform(create("{\"firstName\":\"Carlos\",\"lastName\":\"Gómez\"}").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.status").exists());
		verify(createEmployeeUseCase, never()).create(any(), any());
	}

	@Test
	void unavailableServicesAreBadRequestWithTheRejectedIds() throws Exception {
		given(createEmployeeUseCase.create(eq(USER_ID), any())).willThrow(new InvalidServiceIdsException(List.of(CUT)));

		mockMvc.perform(create(VALID_BODY).with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.rejectedIds[0]").value(CUT.toString()))
			.andExpect(jsonPath("$.errors.serviceIds").exists());
	}

	@Test
	void duplicateEmailIsConflict() throws Exception {
		given(createEmployeeUseCase.create(eq(USER_ID), any())).willThrow(DuplicateEmployeeException.email());

		mockMvc.perform(create(VALID_BODY).with(userJwt()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Another employee of the business already uses this email"));
	}

	@Test
	void unauthenticatedRequestIsUnauthorized() throws Exception {
		mockMvc.perform(create(VALID_BODY)).andExpect(status().isUnauthorized());
		verify(createEmployeeUseCase, never()).create(any(), any());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);

		mockMvc.perform(create(VALID_BODY).with(userJwt()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		verify(createEmployeeUseCase, never()).create(any(), any());
	}

	private static MockHttpServletRequestBuilder create(String body) {
		return post("/api/empleados").contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static RequestPostProcessor userJwt() {
		return jwt().jwt(token -> token.claim(SecurityConfig.USER_ID_CLAIM, USER_ID.toString()));
	}

}
