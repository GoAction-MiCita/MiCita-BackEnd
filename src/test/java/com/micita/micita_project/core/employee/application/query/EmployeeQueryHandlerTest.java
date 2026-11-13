package com.micita.micita_project.core.employee.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.domain.models.EmployeeSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The port HU-12 uses to validate {@code employeeIds}: existence, business and status.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeQueryHandlerTest {

	@Mock
	private EmployeeQueryRepositoryPort employeeQueryRepository;

	@InjectMocks
	private EmployeeQueryHandler handler;

	@Test
	void assignableIdsComeFromTheRepositoryWithoutRepeats() {
		UUID businessId = UUID.randomUUID();
		UUID employeeId = UUID.randomUUID();
		given(employeeQueryRepository.findAssignableIds(businessId, Set.of(employeeId))).willReturn(Set.of(employeeId));

		assertThat(handler.findAssignableIds(businessId, List.of(employeeId, employeeId))).containsExactly(employeeId);
	}

	@Test
	void emptyListNeedsNoLookup() {
		assertThat(handler.findAssignableIds(UUID.randomUUID(), List.of())).isEmpty();
		verifyNoInteractions(employeeQueryRepository);
	}

	@Test
	void summariesCarryOnlyTheIdAndTheName() {
		UUID businessId = UUID.randomUUID();
		UUID employeeId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-29T15:00:00Z");
		given(employeeQueryRepository.findAllOfBusiness(businessId, Set.of(employeeId)))
			.willReturn(List.of(new Employee(employeeId, businessId, "Ana", "Ruiz", "+573001234567", "ana@elcorte.com",
					EmployeeStatus.INACTIVE, now, now)));

		assertThat(handler.findSummaries(businessId, List.of(employeeId, employeeId)))
			.containsExactly(new EmployeeSummary(employeeId, "Ana", "Ruiz"));
	}

	@Test
	void emptyListNeedsNoSummaryLookup() {
		assertThat(handler.findSummaries(UUID.randomUUID(), List.of())).isEmpty();
		verifyNoInteractions(employeeQueryRepository);
	}

	@Test
	void bookableIdsAreTheActiveOnesFromTheRepository() {
		UUID businessId = UUID.randomUUID();
		UUID active = UUID.randomUUID();
		UUID inactive = UUID.randomUUID();
		given(employeeQueryRepository.findBookableIds(businessId, Set.of(active, inactive))).willReturn(Set.of(active));

		assertThat(handler.findBookableIds(businessId, List.of(active, inactive))).containsExactly(active);
	}

	@Test
	void emptyListNeedsNoBookableLookup() {
		assertThat(handler.findBookableIds(UUID.randomUUID(), List.of())).isEmpty();
		verifyNoInteractions(employeeQueryRepository);
	}

}
