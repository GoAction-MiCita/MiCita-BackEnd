package com.micita.micita_project.core.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
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
 * End-to-end HU-22 against a real PostgreSQL: each filter on its own, combined with AND, the 400s and
 * paging over the filtered result.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class EmployeeFilterIntegrationTest {

	private static final Instant T0 = Instant.parse("2026-03-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmployeeJpaRepository employeeJpaRepository;

	@Autowired
	private ServiceJpaRepository serviceJpaRepository;

	@Autowired
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@Test
	void nameMatchesFirstOrLastNameIgnoringCase() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID ana = employee(owner.businessId(), "Ana", "Gómez", EmployeeStatus.ACTIVE, 1);
		UUID mariana = employee(owner.businessId(), "Mariana", "López", EmployeeStatus.INACTIVE, 2);
		UUID santana = employee(owner.businessId(), "Carlos", "Santana", EmployeeStatus.ACTIVE, 3);
		employee(owner.businessId(), "Pedro", "Ruiz", EmployeeStatus.ACTIVE, 4);
		employee(owner.businessId(), "Ana", "Borrada", EmployeeStatus.DELETED, 5);

		assertIds(list(owner, "?name=ANA"), santana, mariana, ana);
		assertIds(list(owner, "?name=%20%20ana%20"), santana, mariana, ana);
		// A LIKE wildcard in the text is taken literally.
		assertIds(list(owner, "?name=%25"));
		// Without filters, the whole (non-deleted) listing comes back.
		list(owner, "").andExpect(jsonPath("$.totalElements").value(4));
	}

	@Test
	void statusFiltersActiveOrInactiveAndRejectsAnythingElse() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID active = employee(owner.businessId(), "Ana", "Gómez", EmployeeStatus.ACTIVE, 1);
		UUID inactive = employee(owner.businessId(), "Carlos", "Ruiz", EmployeeStatus.INACTIVE, 2);
		employee(owner.businessId(), "Borrado", "Ruiz", EmployeeStatus.DELETED, 3);

		assertIds(list(owner, "?status=ACTIVE"), active);
		assertIds(list(owner, "?status=INACTIVE"), inactive);
		list(owner, "?status=DELETED").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.status").exists());
		list(owner, "?status=active").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.status").exists());
		list(owner, "?status=PAUSED").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.status").exists());
	}

	@Test
	void serviceIdsReturnEmployeesProvidingAtLeastOneOfThem() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID shave = service(owner.businessId(), "Afeitado", ServiceStatus.INACTIVE);
		UUID dye = service(owner.businessId(), "Tinte", ServiceStatus.ACTIVE);
		UUID onlyCut = employee(owner.businessId(), "Ana", "Gómez", EmployeeStatus.ACTIVE, 1);
		UUID onlyShave = employee(owner.businessId(), "Carlos", "Ruiz", EmployeeStatus.ACTIVE, 2);
		UUID both = employee(owner.businessId(), "Luis", "Pérez", EmployeeStatus.INACTIVE, 3);
		employee(owner.businessId(), "Pedro", "Díaz", EmployeeStatus.ACTIVE, 4);
		UUID deleted = employee(owner.businessId(), "Borrado", "Díaz", EmployeeStatus.DELETED, 5);
		assign(cut, onlyCut);
		assign(shave, onlyShave);
		assign(cut, both);
		assign(shave, both);
		assign(cut, deleted);

		assertIds(list(owner, "?serviceIds=" + cut + "," + shave), both, onlyShave, onlyCut);
		assertIds(list(owner, "?serviceIds=" + cut + "&serviceIds=" + shave), both, onlyShave, onlyCut);
		assertIds(list(owner, "?serviceIds=" + cut), both, onlyCut);
		assertIds(list(owner, "?serviceIds=" + dye));
	}

	@Test
	void unknownForeignOrDeletedServiceIsBadRequestWithRejectedIds() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID removed = service(owner.businessId(), "Antiguo", ServiceStatus.DELETED);
		UUID foreign = service(other.businessId(), "Ajeno", ServiceStatus.ACTIVE);
		UUID unknown = UUID.randomUUID();

		for (UUID rejected : List.of(removed, foreign, unknown)) {
			list(owner, "?serviceIds=" + cut + "," + rejected).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.serviceIds").exists())
				.andExpect(jsonPath("$.rejectedIds.length()").value(1))
				.andExpect(jsonPath("$.rejectedIds[0]").value(rejected.toString()));
		}
		list(owner, "?serviceIds=not-a-uuid").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.serviceIds").exists());
	}

	@Test
	void filtersAreCombinedWithAnd() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID cut = service(owner.businessId(), "Corte", ServiceStatus.ACTIVE);
		UUID match = employee(owner.businessId(), "Ana", "Gómez", EmployeeStatus.ACTIVE, 1);
		UUID inactive = employee(owner.businessId(), "Ana", "Ruiz", EmployeeStatus.INACTIVE, 2);
		UUID otherName = employee(owner.businessId(), "Carlos", "Pérez", EmployeeStatus.ACTIVE, 3);
		employee(owner.businessId(), "Ana", "Sin Servicio", EmployeeStatus.ACTIVE, 4);
		assign(cut, match);
		assign(cut, inactive);
		assign(cut, otherName);

		assertIds(list(owner, "?name=ana&status=ACTIVE&serviceIds=" + cut), match);
	}

	@Test
	void pagingAndTotalsCountOnlyTheFilteredEmployees() throws Exception {
		Owner owner = ownerWithBusiness();
		UUID oldest = employee(owner.businessId(), "Ana", "Uno", EmployeeStatus.ACTIVE, 1);
		employee(owner.businessId(), "Ana", "Dos", EmployeeStatus.ACTIVE, 2);
		employee(owner.businessId(), "Ana", "Tres", EmployeeStatus.ACTIVE, 3);
		employee(owner.businessId(), "Carlos", "Cuatro", EmployeeStatus.ACTIVE, 4);
		employee(owner.businessId(), "Pedro", "Cinco", EmployeeStatus.ACTIVE, 5);

		list(owner, "?name=ana&page=1&size=2").andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(oldest.toString()));
	}

	@Test
	void filtersNeverReachEmployeesOfOtherBusinesses() throws Exception {
		Owner owner = ownerWithBusiness();
		Owner other = ownerWithBusiness();
		employee(other.businessId(), "Ana", "Ajena", EmployeeStatus.ACTIVE, 1);

		assertIds(list(owner, "?name=ana&status=ACTIVE"));
	}

	private ResultActions list(Owner owner, String query) throws Exception {
		return mockMvc.perform(
				// A URI, so the query (already encoded) is sent as written.
				get(URI.create("/api/employees" + query)).header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.accessToken()));
	}

	private static void assertIds(ResultActions result, UUID... expectedNewestFirst) throws Exception {
		String body = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		List<String> ids = JsonPath.read(body, "$.content[*].id");
		assertThat(ids).containsExactly(Arrays.stream(expectedNewestFirst).map(UUID::toString)
			.toArray(String[]::new));
		assertThat(((Number) JsonPath.read(body, "$.totalElements")).longValue()).isEqualTo(expectedNewestFirst.length);
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

	/**
	 * {@code order} sets the creation time, so a higher one is listed first.
	 */
	private UUID employee(UUID businessId, String firstName, String lastName, EmployeeStatus status, int order) {
		EmployeeEntity employee = new EmployeeEntity();
		employee.setBusinessId(businessId);
		employee.setFirstName(firstName);
		employee.setLastName(lastName);
		employee.setStatus(status);
		employee.setCreatedAt(T0.plusSeconds(order));
		employee.setUpdatedAt(T0.plusSeconds(order));
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
		String username = "filter-" + UUID.randomUUID().toString().substring(0, 8);
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
