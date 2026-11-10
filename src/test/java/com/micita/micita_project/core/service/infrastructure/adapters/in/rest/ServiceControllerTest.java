package com.micita.micita_project.core.service.infrastructure.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.service.application.dto.request.CreateServiceRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;
import com.micita.micita_project.core.service.application.ports.in.CreateServiceUseCase;
import com.micita.micita_project.core.service.domain.exceptions.DuplicateServiceException;
import com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException;
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
 * HU-09 at the web layer: status codes, error bodies, and the business always comes from the access
 * token.
 */
@WebMvcTest(ServiceController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ServiceControllerTest {

	private static final UUID USER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID SERVICE_ID = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final String VALID_BODY = "{\"name\":\"Corte\",\"description\":\"Clásico\",\"durationMinutes\":45,"
			+ "\"price\":35000.00,\"status\":\"ACTIVE\",\"employeeIds\":[\"" + EMPLOYEE_ID + "\"]}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateServiceUseCase createServiceUseCase;

	@MockitoBean
	private BusinessOwnershipQueryUseCase businessOwnershipQuery;

	// SecurityConfig's AuthenticationManager needs one; the JPA implementation is outside the slice. Named so
	// the override also resolves when the context holds more than one UserDetailsService.
	@MockitoBean(name = "userDetailsService")
	private UserDetailsService userDetailsService;

	@BeforeEach
	void userHasABusiness() {
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(true);
	}

	@Test
	void createsServiceForTheCallersBusiness() throws Exception {
		given(createServiceUseCase.create(eq(USER_ID), any())).willReturn(new ServiceResponse(SERVICE_ID, "Corte",
				"Clásico", 45, new BigDecimal("35000.00"), "ACTIVE", List.of(EMPLOYEE_ID),
				Instant.parse("2026-10-29T15:00:00Z")));

		mockMvc.perform(create(VALID_BODY).with(userJwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(SERVICE_ID.toString()))
			.andExpect(jsonPath("$.name").value("Corte"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.price").value(35000.00))
			.andExpect(jsonPath("$.employeeIds[0]").value(EMPLOYEE_ID.toString()));
		verify(createServiceUseCase).create(USER_ID, new CreateServiceRequest("Corte", "Clásico", 45,
				new BigDecimal("35000.00"), "ACTIVE", List.of(EMPLOYEE_ID)));
	}

	@Test
	void businessIdInTheBodyIsIgnored() throws Exception {
		mockMvc.perform(create("{\"businessId\":\"" + UUID.randomUUID() + "\",\"name\":\"Corte\",\"status\":\"ACTIVE\"}")
			.with(userJwt())).andExpect(status().isCreated());
		verify(createServiceUseCase).create(USER_ID, new CreateServiceRequest("Corte", null, null, null, "ACTIVE", null));
	}

	@Test
	void invalidFieldsReturnErrorsPerField() throws Exception {
		mockMvc.perform(create("{\"name\":\"\",\"description\":\"" + "y".repeat(256) + "\",\"durationMinutes\":0,"
				+ "\"price\":-1,\"status\":\"DELETED\"}").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.description").exists())
			.andExpect(jsonPath("$.errors.durationMinutes").exists())
			.andExpect(jsonPath("$.errors.price").exists())
			.andExpect(jsonPath("$.errors.status").value("must be ACTIVE or INACTIVE"));
		verify(createServiceUseCase, never()).create(any(), any());
	}

	@Test
	void missingStatusIsBadRequest() throws Exception {
		mockMvc.perform(create("{\"name\":\"Corte\"}").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.status").exists());
		verify(createServiceUseCase, never()).create(any(), any());
	}

	@Test
	void unavailableEmployeesAreBadRequestWithTheRejectedIds() throws Exception {
		given(createServiceUseCase.create(eq(USER_ID), any()))
			.willThrow(new InvalidEmployeeIdsException(List.of(EMPLOYEE_ID)));

		mockMvc.perform(create(VALID_BODY).with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.rejectedIds[0]").value(EMPLOYEE_ID.toString()))
			.andExpect(jsonPath("$.errors.employeeIds").exists());
	}

	@Test
	void duplicateNameIsConflict() throws Exception {
		given(createServiceUseCase.create(eq(USER_ID), any())).willThrow(new DuplicateServiceException());

		mockMvc.perform(create(VALID_BODY).with(userJwt()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Another service of the business already uses this name"));
	}

	@Test
	void unauthenticatedRequestIsUnauthorized() throws Exception {
		mockMvc.perform(create(VALID_BODY)).andExpect(status().isUnauthorized());
		verify(createServiceUseCase, never()).create(any(), any());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);

		mockMvc.perform(create(VALID_BODY).with(userJwt()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		verify(createServiceUseCase, never()).create(any(), any());
	}

	private static MockHttpServletRequestBuilder create(String body) {
		return post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static RequestPostProcessor userJwt() {
		return jwt().jwt(token -> token.claim(SecurityConfig.USER_ID_CLAIM, USER_ID.toString()));
	}

}
