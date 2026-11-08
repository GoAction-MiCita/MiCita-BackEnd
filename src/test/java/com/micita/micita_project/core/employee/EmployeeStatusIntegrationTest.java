package com.micita.micita_project.core.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.micita.micita_project.TestcontainersConfiguration;
import com.micita.micita_project.core.employee.application.ports.in.BookableEmployeesQueryPort;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities.EmployeeEntity;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end HU-20 against a real PostgreSQL: 200 both ways, idempotency, 400, 404 (unknown, foreign,
 * deleted), service assignments kept and inactive employees not bookable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EmployeeStatusIntegrationTest {

	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmployeeJpaRepository employeeJpaRepository;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Autowired
	private BookableEmployeesQueryPort bookableEmployees;

	@Test
	void deactivatesAndReactivatesKeepingServicesAndBookabilityFollows() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		UUID cut = service(owner.businessId());
		serviceEmployeeJpaRepository.save(new ServiceEmployeeEntity(cut, employeeId));

		changeStatus(owner, employeeId.toString(), "INACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(employeeId.toString()))
			.andExpect(jsonPath("$.status").value("INACTIVE"))
			.andExpect(jsonPath("$.serviceIds[0]").value(cut.toString()));

		EmployeeEntity stored = employeeJpaRepository.findById(employeeId).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
		assertThat(stored.getCreatedAt()).isEqualTo(CREATED);
		assertThat(stored.getUpdatedAt()).isAfter(CREATED);
		assertThat(serviceEmployeeJpaRepository.findAll())
			.anyMatch(row -> row.getServiceId().equals(cut) && row.getEmployeeId().equals(employeeId));
		assertThat(bookableEmployees.findBookableIds(owner.businessId(), List.of(employeeId))).isEmpty();

		changeStatus(owner, employeeId.toString(), "ACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));
		assertThat(bookableEmployees.findBookableIds(owner.businessId(), List.of(employeeId)))
			.containsExactly(employeeId);
	}

	@Test
	void sameStatusIsOkWithoutChanges() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), EmployeeStatus.INACTIVE);

		changeStatus(owner, employeeId.toString(), "INACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("INACTIVE"));

		EmployeeEntity stored = employeeJpaRepository.findById(employeeId).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
		assertThat(stored.getUpdatedAt()).isEqualTo(CREATED);
	}

	@Test
	void deletedOrInvalidStatusIsBadRequest() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), EmployeeStatus.ACTIVE);

		for (String value : List.of("DELETED", "inactive", "PAUSED")) {
			changeStatus(owner, employeeId.toString(), value).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.status").exists());
		}
		changeStatus(owner, "not-a-uuid", "ACTIVE").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.id").exists());
		assertThat(employeeJpaRepository.findById(employeeId).orElseThrow().getStatus())
			.isEqualTo(EmployeeStatus.ACTIVE);
	}

	@Test
	void unknownForeignOrDeletedEmployeeIsNotFound() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID foreign = employee(other.businessId(), EmployeeStatus.ACTIVE);
		UUID deleted = employee(owner.businessId(), EmployeeStatus.DELETED);

		for (UUID id : List.of(UUID.randomUUID(), foreign, deleted)) {
			changeStatus(owner, id.toString(), "INACTIVE").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Employee not found"));
		}
		assertThat(employeeJpaRepository.findById(foreign).orElseThrow().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
		assertThat(employeeJpaRepository.findById(deleted).orElseThrow().getStatus()).isEqualTo(EmployeeStatus.DELETED);
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(patch("/api/employees/" + UUID.randomUUID() + "/status").contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"ACTIVE\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointIsDocumentedInOpenApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/employees/{id}/status'].patch.summary")
				.value("Activate or deactivate an employee"))
			.andExpect(jsonPath("$.paths['/api/employees/{id}/status'].patch.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/employees/{id}/status'].patch.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/employees/{id}/status'].patch.responses['404']").exists());
	}

	private ResultActions changeStatus(Owner owner, String employeeId, String status) throws Exception {
		return mockMvc.perform(patch("/api/employees/" + employeeId + "/status")
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

	private UUID employee(UUID businessId, EmployeeStatus status) {
		EmployeeEntity employee = new EmployeeEntity();
		employee.setBusinessId(businessId);
		employee.setFirstName("Carlos");
		employee.setLastName("Gómez");
		employee.setStatus(status);
		employee.setCreatedAt(CREATED);
		employee.setUpdatedAt(CREATED);
		return employeeJpaRepository.save(employee).getId();
	}

	private UUID service(UUID businessId) {
		ServiceEntity service = new ServiceEntity();
		service.setBusinessId(businessId);
		service.setName("Corte");
		service.setStatus(ServiceStatus.ACTIVE);
		service.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
		service.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
		return serviceJpaRepository.save(service).getId();
	}

	private String newUser() throws Exception {
		String username = "status-" + UUID.randomUUID().toString().substring(0, 8);
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
