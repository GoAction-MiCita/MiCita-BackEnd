package com.micita.micita_project.core.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.micita.micita_project.TestcontainersConfiguration;
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

/**
 * End-to-end HU-16 against a real PostgreSQL: paging, newest-first order, soft-deleted employees and
 * services left out, isolation between businesses, 400, 401 and 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EmployeeListIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmployeeJpaRepository employeeJpaRepository;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Test
	void listsNonDeletedEmployeesNewestFirstWithOnlyNonDeletedServices() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID first = employee(owner.businessId(), "Ana", EmployeeStatus.ACTIVE, Instant.parse("2026-01-01T10:00:00Z"));
		UUID second = employee(owner.businessId(), "Carlos", EmployeeStatus.INACTIVE,
				Instant.parse("2026-01-02T10:00:00Z"));
		employee(owner.businessId(), "Borrada", EmployeeStatus.DELETED, Instant.parse("2026-01-03T10:00:00Z"));
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID deleted = service(owner.businessId(), "Antiguo", ServiceStatus.DELETED);
		assign(cut, first);
		assign(deleted, first);

		String body = mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION,
				"Bearer " + owner.accessToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].id").value(second.toString()))
			.andExpect(jsonPath("$.content[1].id").value(first.toString()))
			.andReturn()
			.getResponse()
			.getContentAsString();

		List<Map<String, Object>> content = JsonPath.read(body, "$.content");
		Map<String, Object> ana = content.stream().filter(e -> first.toString().equals(e.get("id"))).findFirst()
			.orElseThrow();
		List<Map<String, Object>> services = (List<Map<String, Object>>) ana.get("services");
		assertThat(services).hasSize(1);
		assertThat(services.get(0).get("name")).isEqualTo("Corte");
		Map<String, Object> carlos = content.stream().filter(e -> second.toString().equals(e.get("id"))).findFirst()
			.orElseThrow();
		assertThat(carlos.get("status")).isEqualTo("INACTIVE");
	}

	@Test
	void pagesAreSlicedAndSizeIsLimited() throws Exception {
		Owner owner = ownerWithBusiness();
		for (int i = 0; i < 3; i++) {
			employee(owner.businessId(), "Empleado" + i, EmployeeStatus.ACTIVE, Instant.parse("2026-02-0" + (i + 1) + "T10:00:00Z"));
		}

		mockMvc.perform(get("/api/employees?page=1&size=2").header(HttpHeaders.AUTHORIZATION,
				"Bearer " + owner.accessToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].firstName").value("Empleado0"));

		mockMvc.perform(get("/api/employees?size=51").header(HttpHeaders.AUTHORIZATION,
				"Bearer " + owner.accessToken()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.size").exists());
		mockMvc.perform(get("/api/employees?page=-1").header(HttpHeaders.AUTHORIZATION,
				"Bearer " + owner.accessToken()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.page").exists());
	}

	@Test
	void employeesOfOtherBusinessesNeverAppear() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		employee(other.businessId(), "Ajena", EmployeeStatus.ACTIVE, Instant.now());

		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.accessToken()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0))
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		String access = JsonPath.read(login(newUser()), "$.accessToken");

		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
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

	private UUID employee(UUID businessId, String firstName, EmployeeStatus status, Instant createdAt) {
		EmployeeEntity employee = new EmployeeEntity();
		employee.setBusinessId(businessId);
		employee.setFirstName(firstName);
		employee.setLastName("Gómez");
		employee.setStatus(status);
		employee.setCreatedAt(createdAt);
		employee.setUpdatedAt(createdAt);
		return employeeJpaRepository.save(employee).getId();
	}

	private UUID service(UUID businessId, String name, ServiceStatus status) {
		ServiceEntity service = new ServiceEntity();
		service.setBusinessId(businessId);
		service.setName(name);
		service.setStatus(status);
		service.setCreatedAt(Instant.now());
		service.setUpdatedAt(Instant.now());
		return serviceJpaRepository.save(service).getId();
	}

	private void assign(UUID serviceId, UUID employeeId) {
		serviceEmployeeJpaRepository.save(new ServiceEmployeeEntity(serviceId, employeeId));
	}

	private String newUser() throws Exception {
		String username = "lister-" + UUID.randomUUID().toString().substring(0, 8);
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
