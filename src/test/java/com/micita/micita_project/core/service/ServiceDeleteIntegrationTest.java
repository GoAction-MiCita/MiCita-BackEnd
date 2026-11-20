package com.micita.micita_project.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.micita.micita_project.TestcontainersConfiguration;
import com.micita.micita_project.core.service.application.ports.in.BookableServicesQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.FutureAppointmentsQueryPort;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEmployeeEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceEmployeeJpaRepository;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end HU-14 against a real PostgreSQL: 204 soft delete keeping the row and its assignments, 404
 * (unknown, foreign, deleted), 409 with the simulated appointments port, 401, 403, exclusion from the
 * listing, name reuse and Swagger.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ServiceDeleteIntegrationTest {

	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Autowired
	private BookableServicesQueryPort bookableServices;

	// F2 brings the Firestore adapter; until then the port is simulated.
	@MockitoBean
	private FutureAppointmentsQueryPort futureAppointmentsQuery;

	@Test
	void deleteIsSoftKeepsAssignmentsAndHidesTheService() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID serviceId = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID employeeId = UUID.randomUUID();
		serviceEmployeeJpaRepository.save(new ServiceEmployeeEntity(serviceId, employeeId));

		delete(owner, serviceId.toString()).andExpect(status().isNoContent());

		ServiceEntity stored = serviceJpaRepository.findById(serviceId).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(ServiceStatus.DELETED);
		assertThat(stored.getCreatedAt()).isEqualTo(CREATED);
		assertThat(stored.getUpdatedAt()).isAfter(CREATED);
		assertThat(serviceEmployeeJpaRepository.findByEmployeeId(employeeId)).hasSize(1);
		assertThat(bookableServices.findBookableIds(owner.businessId(), List.of(serviceId))).isEmpty();

		mockMvc.perform(get("/api/services").header(HttpHeaders.AUTHORIZATION, owner.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0));
		delete(owner, serviceId.toString()).andExpect(status().isNotFound());
	}

	@Test
	void nameOfADeletedServiceCanBeReused() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID serviceId = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);

		delete(owner, serviceId.toString()).andExpect(status().isNoContent());

		mockMvc.perform(post("/api/services").header(HttpHeaders.AUTHORIZATION, owner.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Corte\",\"status\":\"ACTIVE\"}")).andExpect(status().isCreated());
	}

	@Test
	void serviceWithFutureAppointmentsIsConflictAndKept() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID serviceId = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		given(futureAppointmentsQuery.countFutureAppointmentsOf(any(), any(), any())).willReturn(2L);

		delete(owner, serviceId.toString()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.futureAppointments").value(2));

		ServiceEntity stored = serviceJpaRepository.findById(serviceId).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(ServiceStatus.ACTIVE);
		assertThat(stored.getUpdatedAt()).isEqualTo(CREATED);
	}

	@Test
	void unknownForeignOrDeletedServiceIsNotFound() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID foreign = service(other.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID deleted = service(owner.businessId(), "Tinte", ServiceStatus.DELETED);

		for (UUID id : List.of(UUID.randomUUID(), foreign, deleted)) {
			delete(owner, id.toString()).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Service not found"));
		}
		assertThat(serviceJpaRepository.findById(foreign).orElseThrow().getStatus()).isEqualTo(ServiceStatus.ACTIVE);
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(put("/api/services/" + UUID.randomUUID() + "/delete")).andExpect(status().isUnauthorized());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		String bearer = "Bearer " + JsonPath.read(login(newUser()), "$.accessToken");

		mockMvc.perform(put("/api/services/" + UUID.randomUUID() + "/delete").header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
	}

	@Test
	void endpointIsDocumentedInOpenApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/services/{id}/delete'].put.summary").value("Delete a service"))
			.andExpect(jsonPath("$.paths['/api/services/{id}/delete'].put.responses['204']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}/delete'].put.responses['404']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}/delete'].put.responses['409']").exists());
	}

	private ResultActions delete(Owner owner, String serviceId) throws Exception {
		return mockMvc
			.perform(put("/api/services/" + serviceId + "/delete").header(HttpHeaders.AUTHORIZATION, owner.bearer()));
	}

	private record Owner(String accessToken, UUID businessId) {

		String bearer() {
			return "Bearer " + accessToken;
		}

	}

	private Owner ownerWithBusiness() throws Exception {
		String access = JsonPath.read(login(newUser()), "$.accessToken");
		String business = mockMvc.perform(post("/api/business/onboarding")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"El Corte\",\"address\":\"Calle 10\",\"phone\":\"+573001234567\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return new Owner(access, UUID.fromString(JsonPath.read(business, "$.id")));
	}

	private UUID service(UUID businessId, String name, ServiceStatus status) {
		ServiceEntity service = new ServiceEntity();
		service.setBusinessId(businessId);
		service.setName(name);
		service.setStatus(status);
		service.setCreatedAt(CREATED);
		service.setUpdatedAt(CREATED);
		return serviceJpaRepository.save(service).getId();
	}

	private String newUser() throws Exception {
		String username = "svcdelete-" + UUID.randomUUID().toString().substring(0, 8);
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

}
