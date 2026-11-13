package com.micita.micita_project.core.service.infrastructure.adapters.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.employee.application.ports.in.EmployeeQueryUseCase;
import com.micita.micita_project.core.employee.domain.models.EmployeeSummary;
import com.micita.micita_project.core.service.domain.models.ServiceEmployee;
import com.micita.micita_project.core.service.infrastructure.adapters.out.business.ServiceBusinessQueryAdapter;
import com.micita.micita_project.core.service.infrastructure.adapters.out.employee.ServiceEmployeeQueryModuleAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The service module reaches the business and employee modules only through their input ports (ADR-002).
 */
@ExtendWith(MockitoExtension.class)
class ModuleAdaptersTest {

	@Mock
	private BusinessOwnershipQueryUseCase businessOwnershipQuery;

	@Mock
	private EmployeeQueryUseCase employeeQuery;

	@Test
	void businessIdComesFromTheBusinessModule() {
		UUID userId = UUID.randomUUID();
		UUID businessId = UUID.randomUUID();
		given(businessOwnershipQuery.requireBusinessIdOf(userId)).willReturn(businessId);

		assertThat(new ServiceBusinessQueryAdapter(businessOwnershipQuery).requireBusinessIdOf(userId))
			.isEqualTo(businessId);
	}

	@Test
	void assignableEmployeesComeFromTheEmployeeModule() {
		UUID businessId = UUID.randomUUID();
		UUID employeeId = UUID.randomUUID();
		given(employeeQuery.findAssignableIds(businessId, List.of(employeeId))).willReturn(Set.of(employeeId));

		assertThat(new ServiceEmployeeQueryModuleAdapter(employeeQuery).findAssignableIds(businessId,
				List.of(employeeId)))
			.containsExactly(employeeId);
	}

	@Test
	void employeeNamesComeFromTheEmployeeModule() {
		UUID businessId = UUID.randomUUID();
		UUID employeeId = UUID.randomUUID();
		given(employeeQuery.findSummaries(businessId, List.of(employeeId)))
			.willReturn(List.of(new EmployeeSummary(employeeId, "Ana", "Ruiz")));

		assertThat(new ServiceEmployeeQueryModuleAdapter(employeeQuery).findEmployees(businessId, List.of(employeeId)))
			.containsExactly(new ServiceEmployee(employeeId, "Ana", "Ruiz"));
	}

}
