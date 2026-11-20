package com.micita.micita_project.core.service;

import static org.assertj.core.api.Assertions.assertThat;
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
 * End-to-end HU-12 against a real PostgreSQL: replacement, empty list, repeated ids, 400 with rejected ids
 * and no change, 404, 401, 403, business isolation and Swagger.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ServiceEmployeesIntegrationTest {

	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private EmployeeJpaRepository employeeJpaRepository;

	@Autowired
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Test
	void replacesTheAssignmentIncludingInactiveEmployeesAndKeepingOverlaps() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID service = service(owner.businessId(), ServiceStatus.ACTIVE);
		UUID ana = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		UUID luis = employee(owner.businessId(), EmployeeStatus.INACTIVE);
		UUID marta = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		assign(service, ana, luis);

		replaceEmployees(owner, service.toString(), ids(luis, marta)).andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(service.toString()))
			.andExpect(jsonPath("$.employeeIds.length()").value(2))
			.andExpect(jsonPath("$.employeeIds[0]").value(luis.toString()))
			.andExpect(jsonPath("$.employeeIds[1]").value(marta.toString()));

		assertThat(assigned(service)).containsExactlyInAnyOrder(luis, marta);
		assertThat(serviceJpaRepository.findById(service).orElseThrow().getUpdatedAt()).isEqualTo(CREATED);
	}

	@Test
	void emptyListRemovesEveryEmployee() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID service = service(owner.businessId(), ServiceStatus.INACTIVE);
		UUID ana = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		assign(service, ana);

		replaceEmployees(owner, service.toString(), "[]").andExpect(status().isOk())
			.andExpect(jsonPath("$.employeeIds.length()").value(0));

		assertThat(assigned(service)).isEmpty();
	}

	@Test
	void repeatedIdsAreStoredOnce() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID service = service(owner.businessId(), ServiceStatus.ACTIVE);
		UUID ana = employee(owner.businessId(), EmployeeStatus.ACTIVE);

		replaceEmployees(owner, service.toString(), ids(ana, ana, ana)).andExpect(status().isOk())
			.andExpect(jsonPath("$.employeeIds.length()").value(1));

		assertThat(assigned(service)).containsExactly(ana);
	}

	@Test
	void unavailableEmployeesAreRejectedAndTheAssignmentIsKept() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID service = service(owner.businessId(), ServiceStatus.ACTIVE);
		UUID current = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		UUID valid = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		UUID deleted = employee(owner.businessId(), EmployeeStatus.DELETED);
		UUID foreign = employee(other.businessId(), EmployeeStatus.ACTIVE);
		UUID unknown = UUID.randomUUID();
		assign(service, current);

		replaceEmployees(owner, service.toString(), ids(valid, deleted, foreign, unknown))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.employeeIds").exists())
			.andExpect(jsonPath("$.rejectedIds.length()").value(3))
			.andExpect(jsonPath("$.rejectedIds[0]").value(deleted.toString()))
			.andExpect(jsonPath("$.rejectedIds[1]").value(foreign.toString()))
			.andExpect(jsonPath("$.rejectedIds[2]").value(unknown.toString()));

		assertThat(assigned(service)).containsExactly(current);
	}

	@Test
	void unknownForeignOrDeletedServiceIsNotFoundAndNothingChanges() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID foreign = service(other.businessId(), ServiceStatus.ACTIVE);
		UUID deleted = service(owner.businessId(), ServiceStatus.DELETED);
		UUID ana = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		UUID foreignEmployee = employee(other.businessId(), EmployeeStatus.ACTIVE);
		assign(foreign, foreignEmployee);
		assign(deleted, ana);

		for (UUID id : List.of(UUID.randomUUID(), foreign, deleted)) {
			replaceEmployees(owner, id.toString(), ids(ana)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Service not found"));
		}

		assertThat(assigned(foreign)).containsExactly(foreignEmployee);
		assertThat(assigned(deleted)).containsExactly(ana);
	}

	@Test
	void malformedBodyOrIdIsBadRequest() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID service = service(owner.businessId(), ServiceStatus.ACTIVE);
		UUID ana = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		assign(service, ana);

		for (String body : List.of("{}", "{\"employeeIds\":null}")) {
			mockMvc.perform(put("/api/services/" + service + "/employees")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.employeeIds").exists());
		}
		replaceEmployees(owner, "not-a-uuid", "[]").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.id").exists());
		assertThat(assigned(service)).containsExactly(ana);
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(put("/api/services/" + UUID.randomUUID() + "/employees").contentType(MediaType.APPLICATION_JSON)
			.content("{\"employeeIds\":[]}")).andExpect(status().isUnauthorized());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		String access = JsonPath.read(login(newUser()), "$.accessToken");

		mockMvc.perform(put("/api/services/" + UUID.randomUUID() + "/employees")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"employeeIds\":[]}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
	}

	@Test
	void endpointIsDocumentedInOpenApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/services/{id}/employees'].put.summary")
				.value("Replace the employees of a service"))
			.andExpect(jsonPath("$.paths['/api/services/{id}/employees'].put.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}/employees'].put.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}/employees'].put.responses['404']").exists());
	}

	private ResultActions replaceEmployees(Owner owner, String serviceId, String idsJson) throws Exception {
		return mockMvc.perform(put("/api/services/" + serviceId + "/employees")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.accessToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"employeeIds\":" + idsJson + "}"));
	}

	private static String ids(UUID... ids) {
		return "[" + String.join(",", java.util.Arrays.stream(ids).map(id -> "\"" + id + "\"").toList()) + "]";
	}

	private List<UUID> assigned(UUID serviceId) {
		return serviceEmployeeJpaRepository.findAll()
			.stream()
			.filter(row -> row.getServiceId().equals(serviceId))
			.map(ServiceEmployeeEntity::getEmployeeId)
			.toList();
	}

	private void assign(UUID serviceId, UUID... employeeIds) {
		for (UUID employeeId : employeeIds) {
			serviceEmployeeJpaRepository.save(new ServiceEmployeeEntity(serviceId, employeeId));
		}
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

	private UUID employee(UUID businessId, EmployeeStatus status) {
		EmployeeEntity employee = new EmployeeEntity();
		employee.setBusinessId(businessId);
		employee.setFirstName("Ana");
		employee.setLastName("Ruiz");
		employee.setStatus(status);
		employee.setCreatedAt(Instant.now());
		employee.setUpdatedAt(Instant.now());
		return employeeJpaRepository.save(employee).getId();
	}

	private String newUser() throws Exception {
		String username = "svcemp-" + UUID.randomUUID().toString().substring(0, 8);
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
