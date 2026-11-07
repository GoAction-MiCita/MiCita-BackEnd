package com.micita.micita_project.core.service.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.in.EmployeeServicesQueryUseCase.ServiceSummary;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceEmployeeQueryRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.InvalidServiceIdsException;
import com.micita.micita_project.core.service.domain.models.ServiceAssignment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The services an employee provides, read for the employee listing (HU-16), and the employees that
 * provide some services, for its service filter (HU-22).
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServicesQueryHandlerTest {

	private static final UUID BUSINESS_ID = UUID.randomUUID();

	@Mock
	private ServiceEmployeeQueryRepositoryPort serviceEmployeeQueryRepository;

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@InjectMocks
	private EmployeeServicesQueryHandler handler;

	@Test
	void assignmentsAreGroupedByEmployeeInTheOrderReceived() {
		UUID carlos = UUID.randomUUID();
		UUID ana = UUID.randomUUID();
		UUID cut = UUID.randomUUID();
		UUID shave = UUID.randomUUID();
		given(serviceEmployeeQueryRepository.findActiveAssignments(BUSINESS_ID, List.of(carlos, ana)))
			.willReturn(List.of(new ServiceAssignment(carlos, cut, "Corte"), new ServiceAssignment(carlos, shave,
					"Afeitado"), new ServiceAssignment(ana, cut, "Corte")));

		Map<UUID, List<ServiceSummary>> services = handler.findActiveServicesOf(BUSINESS_ID, List.of(carlos, ana));

		assertThat(services.get(carlos)).containsExactly(new ServiceSummary(cut, "Corte"),
				new ServiceSummary(shave, "Afeitado"));
		assertThat(services.get(ana)).containsExactly(new ServiceSummary(cut, "Corte"));
	}

	@Test
	void employeeWithoutServicesIsAbsentFromTheResult() {
		UUID employeeId = UUID.randomUUID();
		given(serviceEmployeeQueryRepository.findActiveAssignments(BUSINESS_ID, List.of(employeeId)))
			.willReturn(List.of());

		assertThat(handler.findActiveServicesOf(BUSINESS_ID, List.of(employeeId))).doesNotContainKey(employeeId);
	}

	@Test
	void emptyEmployeeListNeedsNoLookup() {
		assertThat(handler.findActiveServicesOf(BUSINESS_ID, List.of())).isEmpty();
		verifyNoInteractions(serviceEmployeeQueryRepository);
	}

	@Test
	void employeesProvidingAnyOfTheServicesAreReturned() {
		UUID cut = UUID.randomUUID();
		UUID shave = UUID.randomUUID();
		UUID carlos = UUID.randomUUID();
		UUID ana = UUID.randomUUID();
		given(serviceQueryRepository.findAssignableIds(BUSINESS_ID, Set.of(cut, shave))).willReturn(Set.of(cut, shave));
		given(serviceEmployeeQueryRepository.findEmployeeIdsAssignedTo(Set.of(cut, shave)))
			.willReturn(Set.of(carlos, ana));

		assertThat(handler.findEmployeeIdsProviding(BUSINESS_ID, List.of(cut, shave, cut)))
			.containsExactlyInAnyOrder(carlos, ana);
	}

	@Test
	void unknownForeignOrDeletedServicesAreRejectedWithTheirIds() {
		UUID cut = UUID.randomUUID();
		UUID deleted = UUID.randomUUID();
		given(serviceQueryRepository.findAssignableIds(BUSINESS_ID, Set.of(cut, deleted))).willReturn(Set.of(cut));

		assertThatThrownBy(() -> handler.findEmployeeIdsProviding(BUSINESS_ID, List.of(cut, deleted)))
			.isInstanceOf(InvalidServiceIdsException.class)
			.satisfies(error -> assertThat(((InvalidServiceIdsException) error).getRejectedIds())
				.containsExactly(deleted));
		verifyNoInteractions(serviceEmployeeQueryRepository);
	}

	@Test
	void noServicesNeedNoLookup() {
		assertThat(handler.findEmployeeIdsProviding(BUSINESS_ID, List.of())).isEmpty();
		verifyNoInteractions(serviceQueryRepository, serviceEmployeeQueryRepository);
	}

}
