package com.micita.micita_project.core.service.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.out.command.ServiceEmployeeCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.EmployeeQueryPort;
import com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-09 (employeeIds): unknown, foreign or deleted employees are rejected and nothing is written.
 */
@ExtendWith(MockitoExtension.class)
class AssignServiceEmployeesCommandHandlerTest {

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID SERVICE_ID = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final UUID ANA = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID LUIS = UUID.fromString("1b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e");

	@Mock
	private EmployeeQueryPort employeeQuery;

	@Mock
	private ServiceEmployeeCommandRepositoryPort serviceEmployeeCommandRepository;

	@InjectMocks
	private AssignServiceEmployeesCommandHandler handler;

	@Test
	void replacesTheEmployeesIgnoringRepeatedIds() {
		given(employeeQuery.findAssignableIds(BUSINESS_ID, Set.of(ANA, LUIS))).willReturn(Set.of(ANA, LUIS));

		Set<UUID> assigned = handler.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(ANA, LUIS, ANA));

		assertThat(assigned).containsExactly(ANA, LUIS);
		verify(serviceEmployeeCommandRepository).replaceEmployeesOfService(SERVICE_ID, Set.of(ANA, LUIS));
	}

	@Test
	void rejectsUnavailableEmployeesAndWritesNothing() {
		given(employeeQuery.findAssignableIds(BUSINESS_ID, Set.of(ANA, LUIS))).willReturn(Set.of(ANA));

		assertThatThrownBy(() -> handler.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(ANA, LUIS)))
			.isInstanceOfSatisfying(InvalidEmployeeIdsException.class, exception -> {
				assertThat(exception.getField()).isEqualTo("employeeIds");
				assertThat(exception.getRejectedIds()).containsExactly(LUIS);
			});
		verify(serviceEmployeeCommandRepository, never()).replaceEmployeesOfService(SERVICE_ID, Set.of(ANA));
	}

	@Test
	void emptyListRemovesEveryEmployeeWithoutQueryingThem() {
		assertThat(handler.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of())).isEmpty();

		verifyNoInteractions(employeeQuery);
		verify(serviceEmployeeCommandRepository).replaceEmployeesOfService(SERVICE_ID, Set.of());
	}

}
