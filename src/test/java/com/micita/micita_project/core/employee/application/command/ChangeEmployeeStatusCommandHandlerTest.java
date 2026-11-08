package com.micita.micita_project.core.employee.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.employee.application.dto.request.ChangeEmployeeStatusRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.mappers.EmployeeMapper;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-20: ACTIVE and INACTIVE switch both ways, the current status is a no-op, and an employee outside
 * the caller's business (or deleted) is not found.
 */
@ExtendWith(MockitoExtension.class)
class ChangeEmployeeStatusCommandHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID CUT = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final UUID BEARD = UUID.fromString("6d3e9f5a-2b4c-4d6e-8f7a-8b9c0d1e2f3a");

	private static final Instant CREATED = Instant.parse("2026-10-01T10:00:00Z");

	private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private EmployeeQueryRepositoryPort employeeQueryRepository;

	@Mock
	private EmployeeCommandRepositoryPort employeeCommandRepository;

	@Mock
	private EmployeeServicesQueryPort employeeServicesQuery;

	private ChangeEmployeeStatusCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new ChangeEmployeeStatusCommandHandler(businessQuery, employeeQueryRepository,
				employeeCommandRepository, employeeServicesQuery, Mappers.getMapper(EmployeeMapper.class),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void deactivatesAnActiveEmployeeKeepingEverythingElse() {
		givenEmployee(EmployeeStatus.ACTIVE);
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(employeeServicesQuery.findActiveServicesOf(BUSINESS_ID, List.of(EMPLOYEE_ID)))
			.willReturn(Map.of(EMPLOYEE_ID, List.of(new EmployeeServiceRef(BEARD, "Barba"),
					new EmployeeServiceRef(CUT, "Corte"))));

		EmployeeResponse response = handler.changeStatus(OWNER_ID, EMPLOYEE_ID,
				new ChangeEmployeeStatusRequest("INACTIVE"));

		ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
		verify(employeeCommandRepository).save(saved.capture());
		assertThat(saved.getValue()).isEqualTo(new Employee(EMPLOYEE_ID, BUSINESS_ID, "Carlos", "Gómez",
				"+573001234567", "carlos@elcorte.com", EmployeeStatus.INACTIVE, CREATED, NOW));
		assertThat(response.status()).isEqualTo("INACTIVE");
		assertThat(response.createdAt()).isEqualTo(CREATED);
		assertThat(response.serviceIds()).containsExactly(BEARD, CUT);
	}

	@Test
	void activatesAnInactiveEmployee() {
		givenEmployee(EmployeeStatus.INACTIVE);
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		EmployeeResponse response = handler.changeStatus(OWNER_ID, EMPLOYEE_ID,
				new ChangeEmployeeStatusRequest("ACTIVE"));

		assertThat(response.status()).isEqualTo("ACTIVE");
		assertThat(response.serviceIds()).isEmpty();
	}

	@Test
	void sameStatusChangesNothing() {
		givenEmployee(EmployeeStatus.INACTIVE);

		EmployeeResponse response = handler.changeStatus(OWNER_ID, EMPLOYEE_ID,
				new ChangeEmployeeStatusRequest("INACTIVE"));

		assertThat(response.status()).isEqualTo("INACTIVE");
		verify(employeeCommandRepository, never()).save(any());
	}

	@Test
	void employeeOutsideTheBusinessOrDeletedIsNotFound() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findOfBusiness(BUSINESS_ID, EMPLOYEE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.changeStatus(OWNER_ID, EMPLOYEE_ID, new ChangeEmployeeStatusRequest("ACTIVE")))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Employee not found");
		verifyNoInteractions(employeeCommandRepository, employeeServicesQuery);
	}

	@Test
	void userWithoutBusinessIsRejectedBeforeAnyLookup() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.changeStatus(OWNER_ID, EMPLOYEE_ID, new ChangeEmployeeStatusRequest("ACTIVE")))
			.isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(employeeQueryRepository, employeeCommandRepository, employeeServicesQuery);
	}

	private void givenEmployee(EmployeeStatus status) {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findOfBusiness(BUSINESS_ID, EMPLOYEE_ID)).willReturn(Optional.of(new Employee(
				EMPLOYEE_ID, BUSINESS_ID, "Carlos", "Gómez", "+573001234567", "carlos@elcorte.com", status, CREATED,
				CREATED)));
	}

}
