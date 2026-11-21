package com.micita.micita_project.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
 * End-to-end HU-10 against a real PostgreSQL: detail, edit, 400 (fields and rejected employees, with
 * rollback), 409 excluding the service itself, replacement of the employees, 404, 401, 403, business
 * isolation and Swagger.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ServiceUpdateIntegrationTest {

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
	void getReturnsTheDetailWithItsNonDeletedEmployees() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID service = service(owner.businessId(), "Corte", ServiceStatus.INACTIVE);
		UUID ana = employee(owner.businessId(), "Ana", EmployeeStatus.ACTIVE);
		UUID luis = employee(owner.businessId(), "Luis", EmployeeStatus.INACTIVE);
		UUID deleted = employee(owner.businessId(), "Borrado", EmployeeStatus.DELETED);
		assign(service, luis, deleted, ana);

		mockMvc.perform(get("/api/services/" + service).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(service.toString()))
			.andExpect(jsonPath("$.name").value("Corte"))
			.andExpect(jsonPath("$.status").value("INACTIVE"))
			.andExpect(jsonPath("$.employees.length()").value(2))
			.andExpect(jsonPath("$.employees[0].id").value(ana.toString()))
			.andExpect(jsonPath("$.employees[1].id").value(luis.toString()))
			.andExpect(jsonPath("$.createdAt").exists())
			.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	void putReplacesDataStatusAndEmployees() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID service = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID ana = employee(owner.businessId(), "Ana", EmployeeStatus.ACTIVE);
		UUID luis = employee(owner.businessId(), "Luis", EmployeeStatus.INACTIVE);
		assign(service, ana);

		putService(owner, service.toString(), body("  Peinado ", "Con brillo", 60, "50000.00", "INACTIVE", ids(luis)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(service.toString()))
			.andExpect(jsonPath("$.name").value("Peinado"))
			.andExpect(jsonPath("$.status").value("INACTIVE"))
			.andExpect(jsonPath("$.employeeIds.length()").value(1))
			.andExpect(jsonPath("$.employeeIds[0]").value(luis.toString()));

		ServiceEntity stored = serviceJpaRepository.findById(service).orElseThrow();
		assertThat(stored.getName()).isEqualTo("Peinado");
		assertThat(stored.getDescription()).isEqualTo("Con brillo");
		assertThat(stored.getDurationMinutes()).isEqualTo(60);
		assertThat(stored.getPrice()).isEqualByComparingTo(new BigDecimal("50000.00"));
		assertThat(stored.getStatus()).isEqualTo(ServiceStatus.INACTIVE);
		assertThat(stored.getCreatedAt()).isEqualTo(CREATED);
		assertThat(stored.getUpdatedAt()).isAfter(CREATED);
		assertThat(assigned(service)).containsExactly(luis);
	}

	@Test
	void emptyEmployeeListRemovesEveryEmployee() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID service = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		assign(service, employee(owner.businessId(), "Ana", EmployeeStatus.ACTIVE));

		putService(owner, service.toString(), body("Corte", null, null, null, "ACTIVE", "[]")).andExpect(status().isOk())
			.andExpect(jsonPath("$.employeeIds.length()").value(0));

		assertThat(assigned(service)).isEmpty();
	}

	@Test
	void keepingItsOwnNameIsNotAConflictButAnotherServicesNameIs() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID service = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		service(owner.businessId(), "Peinado", ServiceStatus.INACTIVE);
		service(owner.businessId(), "Tinte", ServiceStatus.DELETED);
		service(other.businessId(), "Manicure", ServiceStatus.ACTIVE);

		putService(owner, service.toString(), body("CORTE", null, null, null, "ACTIVE", "[]")).andExpect(status().isOk());
		putService(owner, service.toString(), body("peinado", null, null, null, "ACTIVE", "[]"))
			.andExpect(status().isConflict());
		// A deleted service's name and another business's name can be reused.
		putService(owner, service.toString(), body("Tinte", null, null, null, "ACTIVE", "[]")).andExpect(status().isOk());
		putService(owner, service.toString(), body("Manicure", null, null, null, "ACTIVE", "[]")).andExpect(status().isOk());
	}

	@Test
	void invalidDataIsBadRequestAndNothingChanges() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID service = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);

		putService(owner, service.toString(), "{\"name\":\" \",\"status\":\"DELETED\",\"durationMinutes\":0}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.status").exists())
			.andExpect(jsonPath("$.errors.durationMinutes").exists())
			.andExpect(jsonPath("$.errors.employeeIds").exists());
		putService(owner, service.toString(), "{\"name\":\"Peinado\",\"status\":\"ACTIVE\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.employeeIds").exists());
		putService(owner, "not-a-uuid", body("Corte", null, null, null, "ACTIVE", "[]")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.id").exists());

		assertThat(serviceJpaRepository.findById(service).orElseThrow().getName()).isEqualTo("Corte");
	}

	@Test
	void rejectedEmployeesRollTheWholeEditBack() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID service = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID current = employee(owner.businessId(), "Ana", EmployeeStatus.ACTIVE);
		UUID deleted = employee(owner.businessId(), "Borrado", EmployeeStatus.DELETED);
		UUID foreign = employee(other.businessId(), "Ajeno", EmployeeStatus.ACTIVE);
		UUID unknown = UUID.randomUUID();
		assign(service, current);

		putService(owner, service.toString(), body("Peinado", null, null, null, "INACTIVE", ids(deleted, foreign, unknown)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.rejectedIds.length()").value(3));

		ServiceEntity stored = serviceJpaRepository.findById(service).orElseThrow();
		assertThat(stored.getName()).isEqualTo("Corte");
		assertThat(stored.getStatus()).isEqualTo(ServiceStatus.ACTIVE);
		assertThat(assigned(service)).containsExactly(current);
	}

	@Test
	void unknownForeignOrDeletedServiceIsNotFoundForGetAndPut() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID foreign = service(other.businessId(), "Ajeno", ServiceStatus.ACTIVE);
		UUID deleted = service(owner.businessId(), "Borrado", ServiceStatus.DELETED);

		for (UUID id : List.of(UUID.randomUUID(), foreign, deleted)) {
			mockMvc.perform(get("/api/services/" + id).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Service not found"));
			putService(owner, id.toString(), body("Nuevo", null, null, null, "ACTIVE", "[]")).andExpect(status().isNotFound());
		}

		assertThat(serviceJpaRepository.findById(foreign).orElseThrow().getName()).isEqualTo("Ajeno");
		assertThat(serviceJpaRepository.findById(deleted).orElseThrow().getName()).isEqualTo("Borrado");
	}

	@Test
	void requestsWithoutTokenAreUnauthorized() throws Exception {
		mockMvc.perform(get("/api/services/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/services/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
			.content(body("Corte", null, null, null, "ACTIVE", "[]"))).andExpect(status().isUnauthorized());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		String bearer = "Bearer " + JsonPath.read(login(newUser()), "$.accessToken");

		mockMvc.perform(get("/api/services/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		mockMvc.perform(put("/api/services/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("Corte", null, null, null, "ACTIVE", "[]")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
	}

	@Test
	void endpointsAreDocumentedInOpenApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/services/{id}'].get.summary").value("Get a service"))
			.andExpect(jsonPath("$.paths['/api/services/{id}'].get.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}'].get.responses['404']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}'].put.summary").value("Edit a service"))
			.andExpect(jsonPath("$.paths['/api/services/{id}'].put.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}'].put.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}'].put.responses['404']").exists())
			.andExpect(jsonPath("$.paths['/api/services/{id}'].put.responses['409']").exists());
	}

	private ResultActions putService(Owner owner, String serviceId, String body) throws Exception {
		return mockMvc.perform(put("/api/services/" + serviceId).header(HttpHeaders.AUTHORIZATION, owner.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static String body(String name, String description, Integer duration, String price, String status,
			String employeeIdsJson) {
		return "{\"name\":\"" + name + "\"" + (description == null ? "" : ",\"description\":\"" + description + "\"")
				+ (duration == null ? "" : ",\"durationMinutes\":" + duration)
				+ (price == null ? "" : ",\"price\":" + price) + ",\"status\":\"" + status + "\",\"employeeIds\":"
				+ employeeIdsJson + "}";
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

	private UUID employee(UUID businessId, String firstName, EmployeeStatus status) {
		EmployeeEntity employee = new EmployeeEntity();
		employee.setBusinessId(businessId);
		employee.setFirstName(firstName);
		employee.setLastName("Ruiz");
		employee.setStatus(status);
		employee.setCreatedAt(Instant.now());
		employee.setUpdatedAt(Instant.now());
		return employeeJpaRepository.save(employee).getId();
	}

	private String newUser() throws Exception {
		String username = "svcupd-" + UUID.randomUUID().toString().substring(0, 8);
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
