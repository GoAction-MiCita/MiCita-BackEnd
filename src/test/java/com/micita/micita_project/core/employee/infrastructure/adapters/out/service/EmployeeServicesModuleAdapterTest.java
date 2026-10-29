package com.micita.micita_project.core.employee.infrastructure.adapters.out.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.in.AssignEmployeeServicesUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-002: the employee module assigns services only through the service module's port.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServicesModuleAdapterTest {

	@Mock
	private AssignEmployeeServicesUseCase assignEmployeeServices;

	@InjectMocks
	private EmployeeServicesModuleAdapter adapter;

	@Test
	void delegatesToTheServiceModule() {
		UUID businessId = UUID.randomUUID();
		UUID employeeId = UUID.randomUUID();
		UUID serviceId = UUID.randomUUID();
		given(assignEmployeeServices.replaceServicesOfEmployee(businessId, employeeId, List.of(serviceId)))
			.willReturn(Set.of(serviceId));

		assertThat(adapter.replaceServices(businessId, employeeId, List.of(serviceId))).containsExactly(serviceId);
	}

}
