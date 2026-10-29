package com.micita.micita_project.core.service.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.out.command.ServiceEmployeeCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.InvalidServiceIdsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-002 / HU-17: the employee's services are replaced with the list sent, repeated ids count once,
 * and unknown, foreign or deleted services reject the whole list.
 */
@ExtendWith(MockitoExtension.class)
class AssignEmployeeServicesCommandHandlerTest {

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID CUT = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final UUID SHAVE = UUID.fromString("6d3e9f5a-2b4c-4d6e-8f7a-8b9c0d1e2f3a");

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@Mock
	private ServiceEmployeeCommandRepositoryPort serviceEmployeeCommandRepository;

	@InjectMocks
	private AssignEmployeeServicesCommandHandler handler;

	@Test
	void replacesServicesIgnoringRepeatedIds() {
		given(serviceQueryRepository.findAssignableIds(BUSINESS_ID, Set.of(CUT, SHAVE))).willReturn(Set.of(CUT, SHAVE));

		Set<UUID> assigned = handler.replaceServicesOfEmployee(BUSINESS_ID, EMPLOYEE_ID, List.of(CUT, SHAVE, CUT));

		assertThat(assigned).containsExactly(CUT, SHAVE);
		verify(serviceEmployeeCommandRepository).replaceServicesOfEmployee(EMPLOYEE_ID, Set.of(CUT, SHAVE));
	}

	@Test
	void unavailableServicesAreRejectedAndNothingIsStored() {
		given(serviceQueryRepository.findAssignableIds(BUSINESS_ID, Set.of(CUT, SHAVE))).willReturn(Set.of(CUT));

		assertThatThrownBy(() -> handler.replaceServicesOfEmployee(BUSINESS_ID, EMPLOYEE_ID, List.of(CUT, SHAVE)))
			.isInstanceOfSatisfying(InvalidServiceIdsException.class, ex -> {
				assertThat(ex.getField()).isEqualTo("serviceIds");
				assertThat(ex.getRejectedIds()).containsExactly(SHAVE);
			});
		verify(serviceEmployeeCommandRepository, never()).replaceServicesOfEmployee(any(), any());
	}

	@Test
	void emptyListRemovesAllServicesWithoutLookingThemUp() {
		assertThat(handler.replaceServicesOfEmployee(BUSINESS_ID, EMPLOYEE_ID, List.of())).isEmpty();

		verify(serviceQueryRepository, never()).findAssignableIds(any(), anyCollection());
		verify(serviceEmployeeCommandRepository).replaceServicesOfEmployee(EMPLOYEE_ID, Set.of());
	}

}
