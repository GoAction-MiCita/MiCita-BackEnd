package com.micita.micita_project.core.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * End-to-end HU-17 against a real PostgreSQL: 201 with services, 400, 401, 403, 409 and isolation
 * between businesses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EmployeeRegistrationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmployeeJpaRepository employeeJpaRepository;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Test
	void employeeIsCreatedInTheCallersBusinessWithItsServices() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID cut = service(owner.businessId(), ServiceStatus.ACTIVE);
		UUID shave = service(owner.businessId(), ServiceStatus.INACTIVE);

		String body = createEmployee(owner.accessToken(), "{\"businessId\":\"" + UUID.randomUUID() + "\","
				+ "\"firstName\":\" Carlos \",\"lastName\":\"Gómez\",\"phone\":\"+573001234567\","
				+ "\"email\":\"Carlos@ElCorte.com\",\"status\":\"ACTIVE\",\"serviceIds\":[\"" + cut + "\",\"" + shave
				+ "\",\"" + cut + "\"]}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.firstName").value("Carlos"))
			.andExpect(jsonPath("$.email").value("carlos@elcorte.com"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.serviceIds.length()").value(2))
			.andReturn()
			.getResponse()
			.getContentAsString();

		UUID employeeId = UUID.fromString(JsonPath.read(body, "$.id"));
		EmployeeEntity stored = employeeJpaRepository.findById(employeeId).orElseThrow();
		assertThat(stored.getBusinessId()).isEqualTo(owner.businessId());
		assertThat(serviceEmployeeJpaRepository.findByEmployeeId(employeeId)).map(ServiceEmployeeEntity::getServiceId)
			.containsExactlyInAnyOrder(cut, shave);
	}

	@Test
	void invalidFieldsAreBadRequestAndNothingIsStored() throws Exception {
		Owner owner = ownerWithBusiness();

		createEmployee(owner.accessToken(), "{\"firstName\":\"\",\"lastName\":\"" + "x".repeat(101) + "\","
				+ "\"phone\":\"abc\",\"email\":\"bad\",\"status\":\"DELETED\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.firstName").exists())
			.andExpect(jsonPath("$.errors.lastName").exists())
			.andExpect(jsonPath("$.errors.phone").exists())
			.andExpect(jsonPath("$.errors.email").exists())
			.andExpect(jsonPath("$.errors.status").exists());
		createEmployee(owner.accessToken(), "{\"firstName\":\"Carlos\",\"lastName\":\"Gómez\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.status").exists());
		assertThat(employeesOf(owner.businessId())).isEmpty();
	}

	@Test
	void unavailableServicesAreRejectedAndNoEmployeeIsCreated() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID valid = service(owner.businessId(), ServiceStatus.ACTIVE);
		UUID deleted = service(owner.businessId(), ServiceStatus.DELETED);
		UUID foreign = service(other.businessId(), ServiceStatus.ACTIVE);
		UUID unknown = UUID.randomUUID();

		createEmployee(owner.accessToken(), "{\"firstName\":\"Carlos\",\"lastName\":\"Gómez\",\"status\":\"ACTIVE\","
				+ "\"serviceIds\":[\"" + valid + "\",\"" + deleted + "\",\"" + foreign + "\",\"" + unknown + "\"]}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.serviceIds").exists())
			.andExpect(jsonPath("$.rejectedIds.length()").value(3))
			.andExpect(jsonPath("$.rejectedIds[0]").value(deleted.toString()))
			.andExpect(jsonPath("$.rejectedIds[1]").value(foreign.toString()))
			.andExpect(jsonPath("$.rejectedIds[2]").value(unknown.toString()));
		assertThat(employeesOf(owner.businessId())).isEmpty();
	}

	@Test
	void emailOrPhoneOfAnotherEmployeeOfTheBusinessIsConflict() throws Exception {
		Owner owner = ownerWithBusiness();
		createEmployee(owner.accessToken(), employee("Carlos@ElCorte.com", "+573001234567"))
			.andExpect(status().isCreated());

		createEmployee(owner.accessToken(), employee("carlos@elcorte.com", "+573009999999"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Another employee of the business already uses this email"));
		createEmployee(owner.accessToken(), employee("otro@elcorte.com", "+573001234567"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Another employee of the business already uses this phone"));
		assertThat(employeesOf(owner.businessId())).hasSize(1);
	}

	@Test
	void deletedEmployeesDoNotBlockTheirEmailOrPhone() throws Exception {
		Owner owner = ownerWithBusiness();
		EmployeeEntity deleted = new EmployeeEntity();
		deleted.setBusinessId(owner.businessId());
		deleted.setFirstName("Ana");
		deleted.setLastName("Ruiz");
		deleted.setPhone("+573001234567");
		deleted.setEmail("carlos@elcorte.com");
		deleted.setStatus(EmployeeStatus.DELETED);
		deleted.setCreatedAt(Instant.now());
		deleted.setUpdatedAt(Instant.now());
		employeeJpaRepository.save(deleted);

		createEmployee(owner.accessToken(), employee("carlos@elcorte.com", "+573001234567"))
			.andExpect(status().isCreated());
	}

	@Test
	void businessesAreIsolated() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		createEmployee(owner.accessToken(), employee("carlos@elcorte.com", "+573001234567"))
			.andExpect(status().isCreated());

		createEmployee(other.accessToken(), employee("carlos@elcorte.com", "+573001234567"))
			.andExpect(status().isCreated());
		assertThat(employeesOf(owner.businessId())).hasSize(1);
		assertThat(employeesOf(other.businessId())).hasSize(1);
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
			.content(employee("carlos@elcorte.com", null)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		String access = JsonPath.read(login(newUser()), "$.accessToken");

		createEmployee(access, employee("carlos@elcorte.com", null)).andExpect(status().isForbidden())
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

	private UUID service(UUID businessId, ServiceStatus status) {
		ServiceEntity service = new ServiceEntity();
		service.setBusinessId(businessId);
		service.setName("Corte " + UUID.randomUUID());
		service.setStatus(status);
		service.setCreatedAt(Instant.now());
		service.setUpdatedAt(Instant.now());
		return serviceJpaRepository.save(service).getId();
	}

	private List<EmployeeEntity> employeesOf(UUID businessId) {
		return employeeJpaRepository.findAll()
			.stream()
			.filter(employee -> employee.getBusinessId().equals(businessId))
			.toList();
	}

	private static String employee(String email, String phone) {
		return "{\"firstName\":\"Carlos\",\"lastName\":\"Gómez\",\"status\":\"ACTIVE\",\"email\":\"" + email + "\""
				+ (phone == null ? "" : ",\"phone\":\"" + phone + "\"") + "}";
	}

	private ResultActions createEmployee(String accessToken, String body) throws Exception {
		return mockMvc.perform(post("/api/employees").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
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
