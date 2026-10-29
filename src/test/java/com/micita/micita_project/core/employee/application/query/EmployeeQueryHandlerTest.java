package com.micita.micita_project.core.employee.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
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

}
