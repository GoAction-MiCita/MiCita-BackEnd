package com.micita.micita_project.core.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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
 * End-to-end HU-08 against a real PostgreSQL: 200 ordered by name with employees, exclusion of deleted
 * services and employees, isolation between businesses, empty list, paging, 400, 401 and 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ServiceListingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private EmployeeJpaRepository employeeJpaRepository;

	@Autowired
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Test
	void listsServicesByNameWithTheirEmployees() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID tinte = service(owner.businessId(), "tinte", ServiceStatus.INACTIVE);
		UUID corte = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID barba = service(owner.businessId(), "Barba", ServiceStatus.ACTIVE);
		UUID ruiz = employee(owner.businessId(), "Ana", "Ruiz", EmployeeStatus.ACTIVE);
		UUID mora = employee(owner.businessId(), "Ana", "Mora", EmployeeStatus.INACTIVE);
		assign(corte, ruiz);
		assign(corte, mora);
		assign(tinte, ruiz);

		list(owner, "").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(3))
			.andExpect(jsonPath("$.content[0].id").value(barba.toString()))
			.andExpect(jsonPath("$.content[0].employees.length()").value(0))
			.andExpect(jsonPath("$.content[1].id").value(corte.toString()))
			.andExpect(jsonPath("$.content[1].name").value("Corte"))
			.andExpect(jsonPath("$.content[1].description").value("Clásico"))
			.andExpect(jsonPath("$.content[1].durationMinutes").value(45))
			.andExpect(jsonPath("$.content[1].price").value(35000.00))
			.andExpect(jsonPath("$.content[1].status").value("ACTIVE"))
			.andExpect(jsonPath("$.content[1].employees.length()").value(2))
			.andExpect(jsonPath("$.content[1].employees[0].id").value(mora.toString()))
			.andExpect(jsonPath("$.content[1].employees[0].lastName").value("Mora"))
			.andExpect(jsonPath("$.content[1].employees[0].photoUrl").doesNotExist())
			.andExpect(jsonPath("$.content[1].employees[1].id").value(ruiz.toString()))
			.andExpect(jsonPath("$.content[2].id").value(tinte.toString()))
			.andExpect(jsonPath("$.content[2].status").value("INACTIVE"))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(1));
	}

	@Test
	void deletedServicesAndDeletedEmployeesAreLeftOut() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID corte = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID deletedService = service(owner.businessId(), "Antiguo", ServiceStatus.DELETED);
		UUID ana = employee(owner.businessId(), "Ana", "Ruiz", EmployeeStatus.ACTIVE);
		UUID deletedEmployee = employee(owner.businessId(), "Beto", "Gil", EmployeeStatus.DELETED);
		assign(corte, ana);
		assign(corte, deletedEmployee);
		assign(deletedService, ana);

		list(owner, "").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(corte.toString()))
			.andExpect(jsonPath("$.content[0].employees.length()").value(1))
			.andExpect(jsonPath("$.content[0].employees[0].id").value(ana.toString()))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void servicesOfAnotherBusinessNeverAppear() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID mine = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		service(other.businessId(), "Ajeno", ServiceStatus.ACTIVE);
		UUID foreignEmployee = employee(other.businessId(), "Eva", "Paz", EmployeeStatus.ACTIVE);
		assign(mine, foreignEmployee);

		list(owner, "").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(mine.toString()))
			.andExpect(jsonPath("$.content[0].employees.length()").value(0));
	}

	@Test
	void businessWithoutServicesGetsAnEmptyPage() throws Exception {
		Owner owner = ownerWithBusiness();

		list(owner, "").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0))
			.andExpect(jsonPath("$.totalPages").value(0));
	}

	@Test
	void pagesFollowTheNameOrder() throws Exception {
		Owner owner = ownerWithBusiness();
		service(owner.businessId(), "Alfa", ServiceStatus.ACTIVE);
		service(owner.businessId(), "beta", ServiceStatus.ACTIVE);
		service(owner.businessId(), "Gamma", ServiceStatus.ACTIVE);

		list(owner, "?page=1&size=2").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].name").value("Gamma"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void nameFilterMatchesContainedTextIgnoringCase() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID corte = service(owner.businessId(), "Corte clásico", ServiceStatus.ACTIVE);
		UUID escorial = service(owner.businessId(), "ESCORIAL", ServiceStatus.INACTIVE);
		service(owner.businessId(), "Tinte", ServiceStatus.ACTIVE);
		UUID underscore = service(owner.businessId(), "Combo_uno", ServiceStatus.ACTIVE);
		service(owner.businessId(), "Combo duo", ServiceStatus.ACTIVE);

		list(owner, "?name=COR").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].id").value(corte.toString()))
			.andExpect(jsonPath("$.content[1].id").value(escorial.toString()))
			.andExpect(jsonPath("$.totalElements").value(2));
		// LIKE wildcards are literal text, so they don't match everything.
		list(owner, "?name=o_u").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(underscore.toString()));
		list(owner, "?name=zzz").andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void statusFilterReturnsOnlyThatStatus() throws Exception {
		Owner owner = ownerWithBusiness();
		service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID tinte = service(owner.businessId(), "Tinte", ServiceStatus.INACTIVE);
		service(owner.businessId(), "Antiguo", ServiceStatus.DELETED);

		list(owner, "?status=INACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(tinte.toString()));
		list(owner, "?status=ACTIVE").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].name").value("Corte"));
	}

	@Test
	void deletedOrUnknownStatusIsBadRequest() throws Exception {
		Owner owner = ownerWithBusiness();
		service(owner.businessId(), "Antiguo", ServiceStatus.DELETED);

		for (String status : new String[] { "DELETED", "active", "PENDING" }) {
			list(owner, "?status=" + status).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.status").exists());
		}
	}

	@Test
	void employeeFilterReturnsServicesWithAtLeastOneOfTheEmployees() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID corte = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID tinte = service(owner.businessId(), "Tinte", ServiceStatus.ACTIVE);
		UUID barba = service(owner.businessId(), "Barba", ServiceStatus.ACTIVE);
		UUID ana = employee(owner.businessId(), "Ana", "Ruiz", EmployeeStatus.ACTIVE);
		UUID luis = employee(owner.businessId(), "Luis", "Mora", EmployeeStatus.INACTIVE);
		UUID eva = employee(owner.businessId(), "Eva", "Paz", EmployeeStatus.ACTIVE);
		assign(corte, ana);
		assign(tinte, luis);
		assign(tinte, ana);
		assign(barba, eva);

		list(owner, "?employeeIds=" + ana + "," + luis).andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].id").value(corte.toString()))
			.andExpect(jsonPath("$.content[1].id").value(tinte.toString()))
			// Each service still lists all of its employees, not only the filtered ones.
			.andExpect(jsonPath("$.content[1].employees.length()").value(2))
			.andExpect(jsonPath("$.totalElements").value(2));
		list(owner, "?employeeIds=" + ana + "&employeeIds=" + eva).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	void employeeWithoutServicesGivesAnEmptyPage() throws Exception {
		Owner owner = ownerWithBusiness();
		service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID ana = employee(owner.businessId(), "Ana", "Ruiz", EmployeeStatus.ACTIVE);

		list(owner, "?employeeIds=" + ana).andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0))
			.andExpect(jsonPath("$.totalPages").value(0));
	}

	@Test
	void filtersAreCombinedWithAnd() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID corte = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID corteInactivo = service(owner.businessId(), "Corte de barba", ServiceStatus.INACTIVE);
		UUID tinte = service(owner.businessId(), "Tinte", ServiceStatus.ACTIVE);
		UUID ana = employee(owner.businessId(), "Ana", "Ruiz", EmployeeStatus.ACTIVE);
		assign(corte, ana);
		assign(corteInactivo, ana);
		assign(tinte, ana);

		list(owner, "?name=corte&status=ACTIVE&employeeIds=" + ana).andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(corte.toString()));
		list(owner, "?name=tinte&status=INACTIVE&employeeIds=" + ana).andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0));
	}

	@Test
	void totalsAndPagesRespectTheFilters() throws Exception {
		Owner owner = ownerWithBusiness();
		service(owner.businessId(), "Corte A", ServiceStatus.ACTIVE);
		service(owner.businessId(), "Corte B", ServiceStatus.ACTIVE);
		service(owner.businessId(), "Corte C", ServiceStatus.ACTIVE);
		service(owner.businessId(), "Tinte", ServiceStatus.ACTIVE);

		list(owner, "?name=corte&page=1&size=2").andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].name").value("Corte C"))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void unknownForeignOrDeletedEmployeesAreBadRequestWithTheirIds() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID mine = employee(owner.businessId(), "Ana", "Ruiz", EmployeeStatus.ACTIVE);
		UUID foreign = employee(other.businessId(), "Eva", "Paz", EmployeeStatus.ACTIVE);
		UUID deleted = employee(owner.businessId(), "Beto", "Gil", EmployeeStatus.DELETED);
		UUID unknown = UUID.randomUUID();

		list(owner, "?employeeIds=" + mine + "," + foreign + "," + deleted + "," + unknown)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.employeeIds").exists())
			.andExpect(jsonPath("$.rejectedIds.length()").value(3))
			.andExpect(jsonPath("$.rejectedIds[0]").value(foreign.toString()))
			.andExpect(jsonPath("$.rejectedIds[1]").value(deleted.toString()))
			.andExpect(jsonPath("$.rejectedIds[2]").value(unknown.toString()));
	}

	@Test
	void malformedEmployeeIdIsBadRequest() throws Exception {
		Owner owner = ownerWithBusiness();

		list(owner, "?employeeIds=not-a-uuid").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.employeeIds").exists());
	}

	@Test
	void invalidPagingIsBadRequest() throws Exception {
		Owner owner = ownerWithBusiness();

		list(owner, "?page=-1&size=51").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.page").exists())
			.andExpect(jsonPath("$.errors.size").exists());
	}

	@Test
	void requestWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/services")).andExpect(status().isUnauthorized());
	}

	@Test
	void userWithoutBusinessIsForbidden() throws Exception {
		String access = JsonPath.read(login(newUser()), "$.accessToken");

		mockMvc.perform(get("/api/services").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
	}

	private record Owner(String accessToken, UUID businessId) {
	}

	private ResultActions list(Owner owner, String query) throws Exception {
		return mockMvc.perform(
				get("/api/services" + query).header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.accessToken()));
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
		service.setDescription("Clásico");
		service.setDurationMinutes(45);
		service.setPrice(new BigDecimal("35000.00"));
		service.setStatus(status);
		service.setCreatedAt(Instant.now());
		service.setUpdatedAt(Instant.now());
		return serviceJpaRepository.save(service).getId();
	}

	private UUID employee(UUID businessId, String firstName, String lastName, EmployeeStatus status) {
		EmployeeEntity employee = new EmployeeEntity();
		employee.setBusinessId(businessId);
		employee.setFirstName(firstName);
		employee.setLastName(lastName);
		employee.setStatus(status);
		employee.setCreatedAt(Instant.now());
		employee.setUpdatedAt(Instant.now());
		return employeeJpaRepository.save(employee).getId();
	}

	private void assign(UUID serviceId, UUID employeeId) {
		serviceEmployeeJpaRepository.save(new ServiceEmployeeEntity(serviceId, employeeId));
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
