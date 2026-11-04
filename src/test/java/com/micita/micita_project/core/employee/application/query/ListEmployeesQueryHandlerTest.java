package com.micita.micita_project.core.employee.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidPageRequestException;
import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeListItemResponse;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-16: the listing is scoped to the owner's business, paginated (0/10 by default, at most 50) and
 * carries each employee's non-deleted services.
 */
@ExtendWith(MockitoExtension.class)
class ListEmployeesQueryHandlerTest {

	private static final UUID OWNER_ID = UUID.randomUUID();

	private static final UUID BUSINESS_ID = UUID.randomUUID();

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private EmployeeQueryRepositoryPort employeeQueryRepository;

	@Mock
	private EmployeeServicesQueryPort employeeServicesQuery;

	@InjectMocks
	private ListEmployeesQueryHandler handler;

	@Test
	void pageOfTheOwnersBusinessCarriesEachEmployeesServices() {
		UUID carlosId = UUID.randomUUID();
		UUID anaId = UUID.randomUUID();
		Employee carlos = employee(carlosId, "Carlos", EmployeeStatus.ACTIVE);
		Employee ana = employee(anaId, "Ana", EmployeeStatus.INACTIVE);
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findPageOfBusiness(BUSINESS_ID, 0, 10))
			.willReturn(new PageResponse<>(List.of(carlos, ana), 0, 10, 2, 1));
		EmployeeServiceRef cut = new EmployeeServiceRef(UUID.randomUUID(), "Corte");
		given(employeeServicesQuery.findActiveServicesOf(BUSINESS_ID, List.of(carlosId, anaId)))
			.willReturn(Map.of(carlosId, List.of(cut)));

		PageResponse<EmployeeListItemResponse> page = handler.list(OWNER_ID, 0, 10);

		assertThat(page.content()).extracting(EmployeeListItemResponse::firstName).containsExactly("Carlos", "Ana");
		assertThat(page.content().get(0).services()).containsExactly(cut);
		assertThat(page.content().get(0).status()).isEqualTo("ACTIVE");
		assertThat(page.content().get(1).status()).isEqualTo("INACTIVE");
		assertThat(page.content().get(1).services()).isEmpty();
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(1);
	}

	@Test
	void emptyPageNeedsNoServiceLookup() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findPageOfBusiness(BUSINESS_ID, 3, 10))
			.willReturn(new PageResponse<>(List.of(), 3, 10, 0, 0));

		PageResponse<EmployeeListItemResponse> page = handler.list(OWNER_ID, 3, 10);

		assertThat(page.content()).isEmpty();
		assertThat(page.page()).isEqualTo(3);
		verifyNoInteractions(employeeServicesQuery);
	}

	@Test
	void sizeFiftyIsTheLargestAllowedPage() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findPageOfBusiness(BUSINESS_ID, 0, 50))
			.willReturn(new PageResponse<>(List.of(), 0, 50, 0, 0));

		handler.list(OWNER_ID, 0, 50);

		verify(employeeQueryRepository).findPageOfBusiness(eq(BUSINESS_ID), eq(0), eq(50));
	}

	@Test
	void pageOutOfRangeIsRejectedBeforeAnyLookup() {
		assertThatThrownBy(() -> handler.list(OWNER_ID, -1, 51)).isInstanceOf(InvalidPageRequestException.class)
			.satisfies(error -> assertThat(((InvalidPageRequestException) error).getErrors()).containsOnlyKeys("page",
					"size"));
		verifyNoInteractions(businessQuery, employeeQueryRepository, employeeServicesQuery);
	}

	@Test
	void sizeZeroIsRejected() {
		assertThatThrownBy(() -> handler.list(OWNER_ID, 0, 0)).isInstanceOf(InvalidPageRequestException.class)
			.satisfies(error -> assertThat(((InvalidPageRequestException) error).getErrors()).containsOnlyKeys("size"));
	}

	private static Employee employee(UUID id, String firstName, EmployeeStatus status) {
		return new Employee(id, BUSINESS_ID, firstName, "Gómez", null, null, status, Instant.now(), Instant.now());
	}

}
