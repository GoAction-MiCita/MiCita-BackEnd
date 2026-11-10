package com.micita.micita_project.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * End-to-end HU-09 against a real PostgreSQL: 201 with employees, 400, 401, 403, 409 and isolation
 * between businesses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ServiceRegistrationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private EmployeeJpaRepository employeeJpaRepository;

	@Autowired
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Test
	void serviceIsCreatedInTheCallersBusinessWithItsEmployees() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID ana = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		UUID luis = employee(owner.businessId(), EmployeeStatus.INACTIVE);

		String body = createService(owner.accessToken(), "{\"businessId\":\"" + UUID.randomUUID() + "\","
				+ "\"name\":\" Corte \",\"description\":\"Clásico\",\"durationMinutes\":45,\"price\":35000.50,"
				+ "\"status\":\"ACTIVE\",\"employeeIds\":[\"" + ana + "\",\"" + luis + "\",\"" + ana + "\"]}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Corte"))
			.andExpect(jsonPath("$.durationMinutes").value(45))
			.andExpect(jsonPath("$.price").value(35000.50))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.employeeIds.length()").value(2))
			.andReturn()
			.getResponse()
			.getContentAsString();

		UUID serviceId = UUID.fromString(JsonPath.read(body, "$.id"));
		ServiceEntity stored = serviceJpaRepository.findById(serviceId).orElseThrow();
		assertThat(stored.getBusinessId()).isEqualTo(owner.businessId());
		assertThat(stored.getPrice()).isEqualByComparingTo(new BigDecimal("35000.50"));
		assertThat(serviceEmployeeJpaRepository.findAll()).filteredOn(row -> row.getServiceId().equals(serviceId))
			.map(ServiceEmployeeEntity::getEmployeeId)
			.containsExactlyInAnyOrder(ana, luis);
	}

	@Test
	void onlyNameAndStatusAreRequired() throws Exception {
		Owner owner = ownerWithBusiness();

		createService(owner.accessToken(), "{\"name\":\"Corte\",\"status\":\"INACTIVE\"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.description").doesNotExist())
			.andExpect(jsonPath("$.durationMinutes").doesNotExist())
			.andExpect(jsonPath("$.price").doesNotExist())
			.andExpect(jsonPath("$.employeeIds.length()").value(0));
		assertThat(servicesOf(owner.businessId())).hasSize(1);
	}

	@Test
	void invalidFieldsAreBadRequestAndNothingIsStored() throws Exception {
		Owner owner = ownerWithBusiness();

		createService(owner.accessToken(), "{\"name\":\"\",\"description\":\"" + "y".repeat(256)
				+ "\",\"durationMinutes\":0,\"price\":-1,\"status\":\"DELETED\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.description").exists())
			.andExpect(jsonPath("$.errors.durationMinutes").exists())
			.andExpect(jsonPath("$.errors.price").exists())
			.andExpect(jsonPath("$.errors.status").exists());
		createService(owner.accessToken(), "{\"name\":\"Corte\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.status").exists());
		assertThat(servicesOf(owner.businessId())).isEmpty();
	}

	@Test
	void unavailableEmployeesAreRejectedAndNoServiceIsCreated() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID valid = employee(owner.businessId(), EmployeeStatus.ACTIVE);
		UUID deleted = employee(owner.businessId(), EmployeeStatus.DELETED);
		UUID foreign = employee(other.businessId(), EmployeeStatus.ACTIVE);
		UUID unknown = UUID.randomUUID();

		createService(owner.accessToken(), "{\"name\":\"Corte\",\"status\":\"ACTIVE\",\"employeeIds\":[\"" + valid
				+ "\",\"" + deleted + "\",\"" + foreign + "\",\"" + unknown + "\"]}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.employeeIds").exists())
			.andExpect(jsonPath("$.rejectedIds.length()").value(3))
			.andExpect(jsonPath("$.rejectedIds[0]").value(deleted.toString()))
			.andExpect(jsonPath("$.rejectedIds[1]").value(foreign.toString()))
			.andExpect(jsonPath("$.rejectedIds[2]").value(unknown.toString()));
		assertThat(servicesOf(owner.businessId())).isEmpty();
		assertThat(serviceEmployeeJpaRepository.findByEmployeeId(valid)).isEmpty();
	}

	@Test
	void nameOfAnotherServiceOfTheBusinessIsConflictIgnoringCase() throws Exception {
		Owner owner = ownerWithBusiness();
		createService(owner.accessToken(), service("Corte de Cabello")).andExpect(status().isCreated());

		createService(owner.accessToken(), service("corte DE cabello")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Another service of the business already uses this name"));
		assertThat(servicesOf(owner.businessId())).hasSize(1);
	}

	@Test
	void deletedServicesDoNotBlockTheirName() throws Exception {
		Owner owner = ownerWithBusiness();
		ServiceEntity deleted = new ServiceEntity();
		deleted.setBusinessId(owner.businessId());
		deleted.setName("Corte");
		deleted.setStatus(ServiceStatus.DELETED);
		deleted.setCreatedAt(Instant.now());
		deleted.setUpdatedAt(Instant.now());
		serviceJpaRepository.save(deleted);

		createService(owner.accessToken(), service("corte")).andExpect(status().isCreated());
	}

	@Test
	void businessesAreIsolated() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		createService(owner.accessToken(), service("Corte")).andExpect(status().isCreated());

		createService(other.accessToken(), service("Corte")).andExpect(status().isCreated());
		assertThat(servicesOf(owner.businessId())).hasSize(1);
		assertThat(servicesOf(other.businessId())).hasSize(1);
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(service("Corte")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		String access = JsonPath.read(login(newUser()), "$.accessToken");

		createService(access, service("Corte")).andExpect(status().isForbidden())
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

	private List<ServiceEntity> servicesOf(UUID businessId) {
		return serviceJpaRepository.findAll()
			.stream()
			.filter(service -> service.getBusinessId().equals(businessId))
			.toList();
	}

	private static String service(String name) {
		return "{\"name\":\"" + name + "\",\"status\":\"ACTIVE\"}";
	}

	private ResultActions createService(String accessToken, String body) throws Exception {
		return mockMvc.perform(post("/api/services").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private String newUser() throws Exception {
		String username = "owner-" + UUID.randomUUID().toString().substring(0, 8);
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
