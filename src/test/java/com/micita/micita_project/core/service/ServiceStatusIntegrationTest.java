package com.micita.micita_project.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.micita.micita_project.TestcontainersConfiguration;
import com.micita.micita_project.core.service.application.ports.in.BookableServicesQueryPort;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end HU-11 against a real PostgreSQL: 200 both ways, idempotency, 400, 404 (unknown, foreign,
 * deleted), 401, inactive services not bookable and Swagger.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ServiceStatusIntegrationTest {

	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private BookableServicesQueryPort bookableServices;

	@Test
	void deactivatesAndReactivatesAndBookabilityFollows() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID serviceId = service(owner.businessId(), ServiceStatus.ACTIVE);

		changeStatus(owner, serviceId.toString(), "INACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(serviceId.toString()))
			.andExpect(jsonPath("$.status").value("INACTIVE"));

		ServiceEntity stored = serviceJpaRepository.findById(serviceId).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(ServiceStatus.INACTIVE);
		assertThat(stored.getCreatedAt()).isEqualTo(CREATED);
		assertThat(stored.getUpdatedAt()).isAfter(CREATED);
		assertThat(bookableServices.findBookableIds(owner.businessId(), List.of(serviceId))).isEmpty();

		changeStatus(owner, serviceId.toString(), "ACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));
		assertThat(bookableServices.findBookableIds(owner.businessId(), List.of(serviceId)))
			.containsExactly(serviceId);
	}

	@Test
	void sameStatusIsOkWithoutChanges() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID serviceId = service(owner.businessId(), ServiceStatus.INACTIVE);

		changeStatus(owner, serviceId.toString(), "INACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("INACTIVE"));

		ServiceEntity stored = serviceJpaRepository.findById(serviceId).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(ServiceStatus.INACTIVE);
		assertThat(stored.getUpdatedAt()).isEqualTo(CREATED);
	}

	@Test
	void deletedOrInvalidStatusIsBadRequest() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID serviceId = service(owner.businessId(), ServiceStatus.ACTIVE);

		for (String value : List.of("DELETED", "inactive", "PAUSED")) {
			changeStatus(owner, serviceId.toString(), value).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.status").exists());
		}
		changeStatus(owner, "not-a-uuid", "ACTIVE").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.id").exists());
		assertThat(serviceJpaRepository.findById(serviceId).orElseThrow().getStatus()).isEqualTo(ServiceStatus.ACTIVE);
	}

	@Test
	void unknownForeignOrDeletedServiceIsNotFound() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID foreign = service(other.businessId(), ServiceStatus.ACTIVE);
		UUID deleted = service(owner.businessId(), ServiceStatus.DELETED);

		for (UUID id : List.of(UUID.randomUUID(), foreign, deleted)) {
			changeStatus(owner, id.toString(), "INACTIVE").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Service not found"));
		}
		assertThat(serviceJpaRepository.findById(foreign).orElseThrow().getStatus()).isEqualTo(ServiceStatus.ACTIVE);
		assertThat(serviceJpaRepository.findById(deleted).orElseThrow().getStatus()).isEqualTo(ServiceStatus.DELETED);
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(patch("/api/services/" + UUID.randomUUID() + "/status").contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"ACTIVE\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointIsDocumentedInOpenApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/services/{id}/status'].patch.summary")
				.value("Activate or deactivate a service"))
			.andExpect(jsonPath("$.paths['/api/services/{id}/status'].patch.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}/status'].patch.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}/status'].patch.responses['404']").exists());
	}

	private ResultActions changeStatus(Owner owner, String serviceId, String status) throws Exception {
		return mockMvc.perform(patch("/api/services/" + serviceId + "/status")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.accessToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"" + status + "\"}"));
	}

	private record Owner(String accessToken, UUID businessId) {
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

	private UUID service(UUID businessId, ServiceStatus status) {
		ServiceEntity service = new ServiceEntity();
		service.setBusinessId(businessId);
		service.setName("Corte");
		service.setStatus(status);
		service.setCreatedAt(CREATED);
		service.setUpdatedAt(CREATED);
		return serviceJpaRepository.save(service).getId();
	}

	private String newUser() throws Exception {
		String username = "svcstatus-" + UUID.randomUUID().toString().substring(0, 8);
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
