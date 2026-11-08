package com.micita.micita_project.core.employee.infrastructure.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidPageRequestException;
import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.employee.application.dto.request.ChangeEmployeeStatusRequest;
import com.micita.micita_project.core.employee.application.dto.request.CreateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.request.EmployeeListFilter;
import com.micita.micita_project.core.employee.application.dto.request.UpdateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeDetailResponse;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeListItemResponse;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.ports.in.ChangeEmployeeStatusUseCase;
import com.micita.micita_project.core.employee.application.ports.in.CreateEmployeeUseCase;
import com.micita.micita_project.core.employee.application.ports.in.DeleteEmployeeUseCase;
import com.micita.micita_project.core.employee.application.ports.in.GetEmployeeUseCase;
import com.micita.micita_project.core.employee.application.ports.in.ListEmployeesUseCase;
import com.micita.micita_project.core.employee.application.ports.in.UpdateEmployeeUseCase;
import com.micita.micita_project.core.employee.domain.exceptions.DuplicateEmployeeException;
import com.micita.micita_project.core.employee.domain.exceptions.EmployeeHasFutureAppointmentsException;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
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
 * HU-17, HU-16/22, HU-20, HU-18 and HU-21 at the web layer: status codes, error bodies, and the business
 * always comes from the access token.
 */
@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class EmployeeControllerTest {

	private static final UUID USER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID CUT = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final EmployeeListFilter NO_FILTER = EmployeeListFilter.none();

	private static final String VALID_BODY = "{\"firstName\":\"Carlos\",\"lastName\":\"Gómez\","
			+ "\"phone\":\"+573001234567\",\"email\":\"carlos@elcorte.com\",\"status\":\"ACTIVE\","
			+ "\"serviceIds\":[\"" + CUT + "\"]}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateEmployeeUseCase createEmployeeUseCase;

	@MockitoBean
	private ListEmployeesUseCase listEmployeesUseCase;

	@MockitoBean
	private ChangeEmployeeStatusUseCase changeEmployeeStatusUseCase;

	@MockitoBean
	private GetEmployeeUseCase getEmployeeUseCase;

	@MockitoBean
	private UpdateEmployeeUseCase updateEmployeeUseCase;

	@MockitoBean
	private DeleteEmployeeUseCase deleteEmployeeUseCase;

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

	@Test
	void listsEmployeesOfTheCallersBusinessWithTheirServices() throws Exception {
		given(listEmployeesUseCase.list(USER_ID, NO_FILTER, 0, 10)).willReturn(new PageResponse<>(
				List.of(new EmployeeListItemResponse(EMPLOYEE_ID, "Carlos", "Gómez", "+573001234567",
						"carlos@elcorte.com", "ACTIVE", List.of(new EmployeeServiceRef(CUT, "Corte")))),
				0, 10, 1, 1));

		mockMvc.perform(get("/api/employees").with(userJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(EMPLOYEE_ID.toString()))
			.andExpect(jsonPath("$.content[0].services[0].name").value("Corte"))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1));
		verify(listEmployeesUseCase).list(USER_ID, NO_FILTER, 0, 10);
	}

	@Test
	void pageAndSizeArePassedThrough() throws Exception {
		given(listEmployeesUseCase.list(USER_ID, NO_FILTER, 2, 50)).willReturn(new PageResponse<>(List.of(), 2, 50, 0, 0));

		mockMvc.perform(get("/api/employees?page=2&size=50").with(userJwt())).andExpect(status().isOk());
		verify(listEmployeesUseCase).list(USER_ID, NO_FILTER, 2, 50);
	}

	@Test
	void pageOutOfRangeIsBadRequestWithErrorsPerParameter() throws Exception {
		given(listEmployeesUseCase.list(USER_ID, NO_FILTER, -1, 51)).willThrow(new InvalidPageRequestException(
				Map.of("page", "must be greater than or equal to 0", "size", "must be between 1 and 50")));

		mockMvc.perform(get("/api/employees?page=-1&size=51").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.page").exists())
			.andExpect(jsonPath("$.errors.size").exists());
	}

	@Test
	void filtersArePassedThroughWithCommaSeparatedServiceIds() throws Exception {
		UUID shave = UUID.fromString("6d3e9f5a-2b4c-4d6e-8f7a-8b9c0d1e2f3a");
		EmployeeListFilter filter = new EmployeeListFilter("ana", "ACTIVE", List.of(CUT, shave));
		given(listEmployeesUseCase.list(USER_ID, filter, 0, 10)).willReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));

		mockMvc.perform(get("/api/employees?name=ana&status=ACTIVE&serviceIds=" + CUT + "," + shave).with(userJwt()))
			.andExpect(status().isOk());
		verify(listEmployeesUseCase).list(USER_ID, filter, 0, 10);
	}

	@Test
	void invalidStatusIsBadRequestWithErrorsMap() throws Exception {
		EmployeeListFilter filter = new EmployeeListFilter(null, "DELETED", null);
		given(listEmployeesUseCase.list(USER_ID, filter, 0, 10))
			.willThrow(new InvalidPageRequestException(Map.of("status", "must be ACTIVE or INACTIVE")));

		mockMvc.perform(get("/api/employees?status=DELETED").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.status").value("must be ACTIVE or INACTIVE"));
	}

	@Test
	void malformedServiceIdIsBadRequestWithErrorsMap() throws Exception {
		mockMvc.perform(get("/api/employees?serviceIds=not-a-uuid").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.serviceIds").exists());
		verify(listEmployeesUseCase, never()).list(any(), any(), anyInt(), anyInt());
	}

	@Test
	void foreignServiceInFilterIsBadRequestWithRejectedIds() throws Exception {
		EmployeeListFilter filter = new EmployeeListFilter(null, null, List.of(CUT));
		given(listEmployeesUseCase.list(USER_ID, filter, 0, 10)).willThrow(new InvalidServiceIdsException(List.of(CUT)));

		mockMvc.perform(get("/api/employees?serviceIds=" + CUT).with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.serviceIds").exists())
			.andExpect(jsonPath("$.rejectedIds[0]").value(CUT.toString()));
	}

	@Test
	void listWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
		verify(listEmployeesUseCase, never()).list(any(), any(), anyInt(), anyInt());
	}

	@Test
	void listWithoutBusinessIsForbidden() throws Exception {
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);

		mockMvc.perform(get("/api/employees").with(userJwt()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		verify(listEmployeesUseCase, never()).list(any(), any(), anyInt(), anyInt());
	}

	// HU-20: PATCH /api/employees/{id}/status

	@Test
	void changesStatusOfAnEmployeeOfTheCallersBusiness() throws Exception {
		given(changeEmployeeStatusUseCase.changeStatus(USER_ID, EMPLOYEE_ID, new ChangeEmployeeStatusRequest("INACTIVE")))
			.willReturn(new EmployeeResponse(EMPLOYEE_ID, "Carlos", "Gómez", null, null, "INACTIVE", List.of(CUT),
					Instant.parse("2026-10-29T15:00:00Z")));

		mockMvc.perform(changeStatus(EMPLOYEE_ID.toString(), "{\"status\":\"INACTIVE\"}").with(userJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
			.andExpect(jsonPath("$.status").value("INACTIVE"))
			.andExpect(jsonPath("$.serviceIds[0]").value(CUT.toString()));
	}

	@Test
	void deletedOrUnknownStatusIsBadRequestWithErrorsMap() throws Exception {
		for (String body : List.of("{\"status\":\"DELETED\"}", "{\"status\":\"active\"}", "{\"status\":\"PAUSED\"}",
				"{}")) {
			mockMvc.perform(changeStatus(EMPLOYEE_ID.toString(), body).with(userJwt()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.status").exists());
		}
		verify(changeEmployeeStatusUseCase, never()).changeStatus(any(), any(), any());
	}

	@Test
	void malformedEmployeeIdIsBadRequest() throws Exception {
		mockMvc.perform(changeStatus("not-a-uuid", "{\"status\":\"ACTIVE\"}").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.id").exists());
		verify(changeEmployeeStatusUseCase, never()).changeStatus(any(), any(), any());
	}

	@Test
	void unknownEmployeeIsNotFound() throws Exception {
		given(changeEmployeeStatusUseCase.changeStatus(eq(USER_ID), eq(EMPLOYEE_ID), any()))
			.willThrow(new ResourceNotFoundException("Employee not found"));

		mockMvc.perform(changeStatus(EMPLOYEE_ID.toString(), "{\"status\":\"ACTIVE\"}").with(userJwt()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Employee not found"));
	}

	@Test
	void changeStatusWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(changeStatus(EMPLOYEE_ID.toString(), "{\"status\":\"ACTIVE\"}"))
			.andExpect(status().isUnauthorized());
		verify(changeEmployeeStatusUseCase, never()).changeStatus(any(), any(), any());
	}

	@Test
	void changeStatusWithoutBusinessIsForbidden() throws Exception {
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);

		mockMvc.perform(changeStatus(EMPLOYEE_ID.toString(), "{\"status\":\"ACTIVE\"}").with(userJwt()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		verify(changeEmployeeStatusUseCase, never()).changeStatus(any(), any(), any());
	}

	// HU-18: GET and PUT /api/employees/{id}

	@Test
	void getsTheDetailOfAnEmployeeWithItsServices() throws Exception {
		given(getEmployeeUseCase.get(USER_ID, EMPLOYEE_ID)).willReturn(new EmployeeDetailResponse(EMPLOYEE_ID, "Carlos",
				"Gómez", "+573001234567", "carlos@elcorte.com", "ACTIVE", List.of(new EmployeeServiceRef(CUT, "Corte")),
				Instant.parse("2026-10-01T10:00:00Z"), Instant.parse("2026-10-29T15:00:00Z")));

		mockMvc.perform(get("/api/employees/" + EMPLOYEE_ID).with(userJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
			.andExpect(jsonPath("$.email").value("carlos@elcorte.com"))
			.andExpect(jsonPath("$.services[0].id").value(CUT.toString()))
			.andExpect(jsonPath("$.services[0].name").value("Corte"))
			.andExpect(jsonPath("$.updatedAt").value("2026-10-29T15:00:00Z"));
	}

	@Test
	void getUnknownEmployeeIsNotFound() throws Exception {
		given(getEmployeeUseCase.get(USER_ID, EMPLOYEE_ID)).willThrow(new ResourceNotFoundException("Employee not found"));

		mockMvc.perform(get("/api/employees/" + EMPLOYEE_ID).with(userJwt()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Employee not found"));
	}

	@Test
	void getWithMalformedIdIsBadRequest() throws Exception {
		mockMvc.perform(get("/api/employees/not-a-uuid").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.id").exists());
		verify(getEmployeeUseCase, never()).get(any(), any());
	}

	@Test
	void getWithoutTokenIsUnauthorizedAndWithoutBusinessIsForbidden() throws Exception {
		mockMvc.perform(get("/api/employees/" + EMPLOYEE_ID)).andExpect(status().isUnauthorized());
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);
		mockMvc.perform(get("/api/employees/" + EMPLOYEE_ID).with(userJwt()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		verify(getEmployeeUseCase, never()).get(any(), any());
	}

	@Test
	void updatesAnEmployeeOfTheCallersBusiness() throws Exception {
		given(updateEmployeeUseCase.update(eq(USER_ID), eq(EMPLOYEE_ID), any())).willReturn(new EmployeeResponse(
				EMPLOYEE_ID, "Carlos", "Gómez", "+573001234567", "carlos@elcorte.com", "INACTIVE", List.of(CUT),
				Instant.parse("2026-10-01T10:00:00Z")));

		mockMvc.perform(update(EMPLOYEE_ID.toString(), VALID_BODY.replace("ACTIVE", "INACTIVE")).with(userJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
			.andExpect(jsonPath("$.status").value("INACTIVE"))
			.andExpect(jsonPath("$.serviceIds[0]").value(CUT.toString()));
		verify(updateEmployeeUseCase).update(USER_ID, EMPLOYEE_ID, new UpdateEmployeeRequest("Carlos", "Gómez",
				"+573001234567", "carlos@elcorte.com", "INACTIVE", List.of(CUT)));
	}

	@Test
	void updateWithoutServiceIdsPassesNullSoServicesAreKept() throws Exception {
		mockMvc.perform(update(EMPLOYEE_ID.toString(), "{\"firstName\":\"Carlos\",\"lastName\":\"Gómez\","
				+ "\"status\":\"ACTIVE\"}").with(userJwt())).andExpect(status().isOk());
		verify(updateEmployeeUseCase).update(USER_ID, EMPLOYEE_ID,
				new UpdateEmployeeRequest("Carlos", "Gómez", null, null, "ACTIVE", null));
	}

	@Test
	void updateWithInvalidFieldsIsBadRequestWithErrorsPerField() throws Exception {
		mockMvc.perform(update(EMPLOYEE_ID.toString(), "{\"firstName\":\"\",\"lastName\":\"Gómez\","
				+ "\"phone\":\"12-34\",\"email\":\"bad\",\"status\":\"DELETED\"}").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.firstName").exists())
			.andExpect(jsonPath("$.errors.phone").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.status").value("must be ACTIVE or INACTIVE"));
		verify(updateEmployeeUseCase, never()).update(any(), any(), any());
	}

	@Test
	void updateWithUnavailableServicesIsBadRequestWithRejectedIds() throws Exception {
		given(updateEmployeeUseCase.update(eq(USER_ID), eq(EMPLOYEE_ID), any()))
			.willThrow(new InvalidServiceIdsException(List.of(CUT)));

		mockMvc.perform(update(EMPLOYEE_ID.toString(), VALID_BODY).with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.rejectedIds[0]").value(CUT.toString()))
			.andExpect(jsonPath("$.errors.serviceIds").exists());
	}

	@Test
	void updateWithThePhoneOfAnotherEmployeeIsConflict() throws Exception {
		given(updateEmployeeUseCase.update(eq(USER_ID), eq(EMPLOYEE_ID), any()))
			.willThrow(DuplicateEmployeeException.phone());

		mockMvc.perform(update(EMPLOYEE_ID.toString(), VALID_BODY).with(userJwt()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Another employee of the business already uses this phone"));
	}

	@Test
	void updateOfUnknownEmployeeIsNotFound() throws Exception {
		given(updateEmployeeUseCase.update(eq(USER_ID), eq(EMPLOYEE_ID), any()))
			.willThrow(new ResourceNotFoundException("Employee not found"));

		mockMvc.perform(update(EMPLOYEE_ID.toString(), VALID_BODY).with(userJwt()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Employee not found"));
	}

	@Test
	void updateWithoutTokenIsUnauthorizedAndWithoutBusinessIsForbidden() throws Exception {
		mockMvc.perform(update(EMPLOYEE_ID.toString(), VALID_BODY)).andExpect(status().isUnauthorized());
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);
		mockMvc.perform(update(EMPLOYEE_ID.toString(), VALID_BODY).with(userJwt()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		verify(updateEmployeeUseCase, never()).update(any(), any(), any());
	}

	// HU-21: PUT /api/employees/{id}/delete

	@Test
	void deletesAnEmployeeWithNoContent() throws Exception {
		mockMvc.perform(delete(EMPLOYEE_ID.toString()).with(userJwt())).andExpect(status().isNoContent());
		verify(deleteEmployeeUseCase).delete(USER_ID, EMPLOYEE_ID);
	}

	@Test
	void deleteWithFutureAppointmentsIsConflictWithTheCount() throws Exception {
		doThrow(new EmployeeHasFutureAppointmentsException(2)).when(deleteEmployeeUseCase).delete(USER_ID, EMPLOYEE_ID);

		mockMvc.perform(delete(EMPLOYEE_ID.toString()).with(userJwt()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.futureAppointments").value(2));
	}

	@Test
	void deleteOfUnknownEmployeeIsNotFound() throws Exception {
		doThrow(new ResourceNotFoundException("Employee not found")).when(deleteEmployeeUseCase)
			.delete(USER_ID, EMPLOYEE_ID);

		mockMvc.perform(delete(EMPLOYEE_ID.toString()).with(userJwt()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Employee not found"));
	}

	@Test
	void deleteWithMalformedIdIsBadRequest() throws Exception {
		mockMvc.perform(delete("not-a-uuid").with(userJwt()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.id").exists());
		verify(deleteEmployeeUseCase, never()).delete(any(), any());
	}

	@Test
	void deleteWithoutTokenIsUnauthorizedAndWithoutBusinessIsForbidden() throws Exception {
		mockMvc.perform(delete(EMPLOYEE_ID.toString())).andExpect(status().isUnauthorized());
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);
		mockMvc.perform(delete(EMPLOYEE_ID.toString()).with(userJwt()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		verify(deleteEmployeeUseCase, never()).delete(any(), any());
	}

	private static MockHttpServletRequestBuilder update(String id, String body) {
		return put("/api/employees/" + id).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static MockHttpServletRequestBuilder delete(String id) {
		return put("/api/employees/" + id + "/delete");
	}

	private static MockHttpServletRequestBuilder changeStatus(String id, String body) {
		return patch("/api/employees/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static MockHttpServletRequestBuilder create(String body) {
		return post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static RequestPostProcessor userJwt() {
		return jwt().jwt(token -> token.claim(SecurityConfig.USER_ID_CLAIM, USER_ID.toString()));
	}

}
