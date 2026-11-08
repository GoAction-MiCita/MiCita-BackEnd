package com.micita.micita_project.core.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * End-to-end HU-18 (detail and edit) and HU-21 (soft delete) against a real PostgreSQL: 200, 204, 400,
 * 404 (unknown, foreign, deleted), 409, service replacement, rollback and business isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EmployeeEditIntegrationTest {

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

	// HU-18: GET /api/employees/{id}

	@Test
	void detailHasTheEmployeeAndItsNonDeletedServices() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), "carlos@elcorte.com", "+573001234567", EmployeeStatus.ACTIVE);
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID gone = service(owner.businessId(), "Tinte", ServiceStatus.DELETED);
		assign(employeeId, cut, gone);

		mockMvc.perform(get("/api/employees/" + employeeId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(employeeId.toString()))
			.andExpect(jsonPath("$.email").value("carlos@elcorte.com"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.services.length()").value(1))
			.andExpect(jsonPath("$.services[0].id").value(cut.toString()))
			.andExpect(jsonPath("$.services[0].name").value("Corte"));
	}

	@Test
	void detailOfUnknownForeignOrDeletedEmployeeIsNotFound() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID foreign = employee(other.businessId(), null, null, EmployeeStatus.ACTIVE);
		UUID deleted = employee(owner.businessId(), null, null, EmployeeStatus.DELETED);

		for (UUID id : List.of(UUID.randomUUID(), foreign, deleted)) {
			mockMvc.perform(get("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Employee not found"));
		}
	}

	// HU-18: PUT /api/employees/{id}

	@Test
	void updatesDataStatusAndReplacesServices() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), "carlos@elcorte.com", "+573001234567", EmployeeStatus.ACTIVE);
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID beard = service(owner.businessId(), "Barba", ServiceStatus.INACTIVE);
		assign(employeeId, cut);

		update(owner, employeeId.toString(), body("Andrés", "+573109876543", "Andres@ElCorte.com", "INACTIVE",
				"[\"" + beard + "\"]"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(employeeId.toString()))
			.andExpect(jsonPath("$.firstName").value("Andrés"))
			.andExpect(jsonPath("$.email").value("andres@elcorte.com"))
			.andExpect(jsonPath("$.status").value("INACTIVE"))
			.andExpect(jsonPath("$.serviceIds.length()").value(1))
			.andExpect(jsonPath("$.serviceIds[0]").value(beard.toString()));

		EmployeeEntity stored = employeeJpaRepository.findById(employeeId).orElseThrow();
		assertThat(stored.getFirstName()).isEqualTo("Andrés");
		assertThat(stored.getPhone()).isEqualTo("+573109876543");
		assertThat(stored.getEmail()).isEqualTo("andres@elcorte.com");
		assertThat(stored.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
		assertThat(stored.getCreatedAt()).isEqualTo(CREATED);
		assertThat(stored.getUpdatedAt()).isAfter(CREATED);
		assertThat(servicesOf(employeeId)).containsExactly(beard);
	}

	@Test
	void keepingItsOwnEmailAndPhoneIsNotAConflict() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), "carlos@elcorte.com", "+573001234567", EmployeeStatus.ACTIVE);

		update(owner, employeeId.toString(), body("Carlos", "+573001234567", "CARLOS@elcorte.com", "ACTIVE", null))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("carlos@elcorte.com"));
	}

	@Test
	void emailOrPhoneOfAnotherEmployeeIsConflictButADeletedOneDoesNotCount() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), "carlos@elcorte.com", "+573001234567", EmployeeStatus.ACTIVE);
		employee(owner.businessId(), "ana@elcorte.com", "+573109876543", EmployeeStatus.INACTIVE);
		employee(owner.businessId(), "old@elcorte.com", "+573155555555", EmployeeStatus.DELETED);

		update(owner, employeeId.toString(), body("Carlos", null, "ana@elcorte.com", "ACTIVE", null))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Another employee of the business already uses this email"));
		update(owner, employeeId.toString(), body("Carlos", "+573109876543", null, "ACTIVE", null))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Another employee of the business already uses this phone"));
		assertThat(employeeJpaRepository.findById(employeeId).orElseThrow().getEmail()).isEqualTo("carlos@elcorte.com");

		update(owner, employeeId.toString(), body("Carlos", "+573155555555", "old@elcorte.com", "ACTIVE", null))
			.andExpect(status().isOk());
	}

	@Test
	void emptyServiceListRemovesAllAndMissingListKeepsThem() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), null, null, EmployeeStatus.ACTIVE);
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		assign(employeeId, cut);

		update(owner, employeeId.toString(), body("Carlos", null, null, "ACTIVE", null)).andExpect(status().isOk())
			.andExpect(jsonPath("$.serviceIds[0]").value(cut.toString()));
		assertThat(servicesOf(employeeId)).containsExactly(cut);

		update(owner, employeeId.toString(), body("Carlos", null, null, "ACTIVE", "[]")).andExpect(status().isOk())
			.andExpect(jsonPath("$.serviceIds.length()").value(0));
		assertThat(servicesOf(employeeId)).isEmpty();
	}

	@Test
	void unavailableServiceIsBadRequestAndNothingIsStored() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), "carlos@elcorte.com", null, EmployeeStatus.ACTIVE);
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID deleted = service(owner.businessId(), "Tinte", ServiceStatus.DELETED);
		UUID foreign = service(other.businessId(), "Corte", ServiceStatus.ACTIVE);
		assign(employeeId, cut);

		for (UUID rejected : List.of(deleted, foreign, UUID.randomUUID())) {
			update(owner, employeeId.toString(),
					body("Andrés", null, "andres@elcorte.com", "INACTIVE", "[\"" + rejected + "\"]"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.rejectedIds[0]").value(rejected.toString()));
		}
		EmployeeEntity stored = employeeJpaRepository.findById(employeeId).orElseThrow();
		assertThat(stored.getFirstName()).isEqualTo("Carlos");
		assertThat(stored.getEmail()).isEqualTo("carlos@elcorte.com");
		assertThat(stored.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
		assertThat(servicesOf(employeeId)).containsExactly(cut);
	}

	@Test
	void invalidDataIsBadRequest() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), null, null, EmployeeStatus.ACTIVE);

		update(owner, employeeId.toString(), body("", "12-34", "bad", "DELETED", null))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.firstName").exists())
			.andExpect(jsonPath("$.errors.phone").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.status").exists());
		update(owner, "not-a-uuid", body("Carlos", null, null, "ACTIVE", null)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.id").exists());
		assertThat(employeeJpaRepository.findById(employeeId).orElseThrow().getUpdatedAt()).isEqualTo(CREATED);
	}

	@Test
	void updateOfUnknownForeignOrDeletedEmployeeIsNotFound() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID foreign = employee(other.businessId(), null, null, EmployeeStatus.ACTIVE);
		UUID deleted = employee(owner.businessId(), null, null, EmployeeStatus.DELETED);

		for (UUID id : List.of(UUID.randomUUID(), foreign, deleted)) {
			update(owner, id.toString(), body("Hacker", null, null, "ACTIVE", null)).andExpect(status().isNotFound());
		}
		assertThat(employeeJpaRepository.findById(foreign).orElseThrow().getFirstName()).isEqualTo("Carlos");
		assertThat(employeeJpaRepository.findById(deleted).orElseThrow().getStatus()).isEqualTo(EmployeeStatus.DELETED);
	}

	// HU-21: PUT /api/employees/{id}/delete

	@Test
	void deleteIsSoftKeepsAssignmentsAndHidesTheEmployee() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), "carlos@elcorte.com", "+573001234567", EmployeeStatus.ACTIVE);
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		assign(employeeId, cut);

		delete(owner, employeeId.toString()).andExpect(status().isNoContent());

		EmployeeEntity stored = employeeJpaRepository.findById(employeeId).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(EmployeeStatus.DELETED);
		assertThat(stored.getUpdatedAt()).isAfter(CREATED);
		assertThat(servicesOf(employeeId)).containsExactly(cut);
		assertThat(bookableEmployees.findBookableIds(owner.businessId(), List.of(employeeId))).isEmpty();

		mockMvc.perform(get("/api/employees/" + employeeId).header(HttpHeaders.AUTHORIZATION, owner.bearer()))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, owner.bearer()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0));
		delete(owner, employeeId.toString()).andExpect(status().isNotFound());
	}

	@Test
	void emailAndPhoneOfADeletedEmployeeCanBeReused() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID employeeId = employee(owner.businessId(), "carlos@elcorte.com", "+573001234567", EmployeeStatus.ACTIVE);

		delete(owner, employeeId.toString()).andExpect(status().isNoContent());

		mockMvc.perform(post("/api/employees").header(HttpHeaders.AUTHORIZATION, owner.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("Carlos", "+573001234567", "carlos@elcorte.com", "ACTIVE", null)))
			.andExpect(status().isCreated());
	}

	@Test
	void deleteOfUnknownOrForeignEmployeeIsNotFound() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID foreign = employee(other.businessId(), null, null, EmployeeStatus.ACTIVE);

		for (UUID id : List.of(UUID.randomUUID(), foreign)) {
			delete(owner, id.toString()).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Employee not found"));
		}
		assertThat(employeeJpaRepository.findById(foreign).orElseThrow().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
	}

	// Both stories

	@Test
	void requestsWithoutTokenAreUnauthorized() throws Exception {
		UUID id = UUID.randomUUID();
		mockMvc.perform(get("/api/employees/" + id)).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/employees/" + id).contentType(MediaType.APPLICATION_JSON)
			.content(body("Carlos", null, null, "ACTIVE", null))).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/employees/" + id + "/delete")).andExpect(status().isUnauthorized());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		String bearer = "Bearer " + JsonPath.read(login(newUser()), "$.accessToken");
		UUID id = UUID.randomUUID();

		mockMvc.perform(get("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
		mockMvc.perform(put("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("Carlos", null, null, "ACTIVE", null)))
			.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/employees/" + id + "/delete").header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(status().isForbidden());
	}

	@Test
	void endpointsAreDocumentedInOpenApi() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/employees/{id}'].get.summary").value("Get an employee"))
			.andExpect(jsonPath("$.paths['/api/employees/{id}'].get.responses['404']").exists())
			.andExpect(jsonPath("$.paths['/api/employees/{id}'].put.summary").value("Update an employee"))
			.andExpect(jsonPath("$.paths['/api/employees/{id}'].put.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/employees/{id}'].put.responses['409']").exists())
			.andExpect(jsonPath("$.paths['/api/employees/{id}/delete'].put.summary").value("Delete an employee"))
			.andExpect(jsonPath("$.paths['/api/employees/{id}/delete'].put.responses['204']").exists())
			.andExpect(jsonPath("$.paths['/api/employees/{id}/delete'].put.responses['409']").exists());
	}

	private ResultActions update(Owner owner, String employeeId, String body) throws Exception {
		return mockMvc.perform(put("/api/employees/" + employeeId).header(HttpHeaders.AUTHORIZATION, owner.bearer())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions delete(Owner owner, String employeeId) throws Exception {
		return mockMvc
			.perform(put("/api/employees/" + employeeId + "/delete").header(HttpHeaders.AUTHORIZATION, owner.bearer()));
	}

	private static String body(String firstName, String phone, String email, String status, String serviceIds) {
		return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"Gómez\"" + field("phone", phone)
				+ field("email", email) + ",\"status\":\"" + status + "\""
				+ (serviceIds == null ? "" : ",\"serviceIds\":" + serviceIds) + "}";
	}

	private static String field(String name, String value) {
		return value == null ? "" : ",\"" + name + "\":\"" + value + "\"";
	}

	private Set<UUID> servicesOf(UUID employeeId) {
		return serviceEmployeeJpaRepository.findAll()
			.stream()
			.filter(row -> row.getEmployeeId().equals(employeeId))
			.map(ServiceEmployeeEntity::getServiceId)
			.collect(Collectors.toSet());
	}

	private void assign(UUID employeeId, UUID... serviceIds) {
		for (UUID serviceId : serviceIds) {
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

	private UUID employee(UUID businessId, String email, String phone, EmployeeStatus status) {
		EmployeeEntity employee = new EmployeeEntity();
		employee.setBusinessId(businessId);
		employee.setFirstName("Carlos");
		employee.setLastName("Gómez");
		employee.setEmail(email);
		employee.setPhone(phone);
		employee.setStatus(status);
		employee.setCreatedAt(CREATED);
		employee.setUpdatedAt(CREATED);
		return employeeJpaRepository.save(employee).getId();
	}

	private UUID service(UUID businessId, String name, ServiceStatus status) {
		ServiceEntity service = new ServiceEntity();
		service.setBusinessId(businessId);
		service.setName(name);
		service.setStatus(status);
		service.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
		service.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
		return serviceJpaRepository.save(service).getId();
	}

	private String newUser() throws Exception {
		String username = "edit-" + UUID.randomUUID().toString().substring(0, 8);
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
