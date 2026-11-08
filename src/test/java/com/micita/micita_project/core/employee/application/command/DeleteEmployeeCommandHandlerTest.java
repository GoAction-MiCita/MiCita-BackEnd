package com.micita.micita_project.core.employee.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.FutureAppointmentsQueryPort;
import com.micita.micita_project.core.employee.domain.exceptions.EmployeeHasFutureAppointmentsException;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-21: soft delete keeping the row, 409 with the count when there are future appointments, and 404
 * for unknown, foreign or already deleted employees.
 */
@ExtendWith(MockitoExtension.class)
class DeleteEmployeeCommandHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final Instant CREATED = Instant.parse("2026-10-01T10:00:00Z");

	private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private EmployeeQueryRepositoryPort employeeQueryRepository;

	@Mock
	private EmployeeCommandRepositoryPort employeeCommandRepository;

	@Mock
	private FutureAppointmentsQueryPort futureAppointmentsQuery;

	private DeleteEmployeeCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new DeleteEmployeeCommandHandler(businessQuery, employeeQueryRepository, employeeCommandRepository,
				futureAppointmentsQuery, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void marksTheEmployeeDeletedKeepingEverythingElse() {
		givenEmployee(EmployeeStatus.INACTIVE);
		given(futureAppointmentsQuery.countFutureAppointmentsOf(BUSINESS_ID, EMPLOYEE_ID, NOW)).willReturn(0L);

		handler.delete(OWNER_ID, EMPLOYEE_ID);

		ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
		verify(employeeCommandRepository).save(saved.capture());
		assertThat(saved.getValue()).isEqualTo(new Employee(EMPLOYEE_ID, BUSINESS_ID, "Carlos", "Gómez",
				"+573001234567", "carlos@elcorte.com", EmployeeStatus.DELETED, CREATED, NOW));
	}

	@Test
	void employeeWithFutureAppointmentsIsConflictWithTheCount() {
		givenEmployee(EmployeeStatus.ACTIVE);
		given(futureAppointmentsQuery.countFutureAppointmentsOf(BUSINESS_ID, EMPLOYEE_ID, NOW)).willReturn(3L);

		assertThatThrownBy(() -> handler.delete(OWNER_ID, EMPLOYEE_ID))
			.isInstanceOfSatisfying(EmployeeHasFutureAppointmentsException.class, ex -> {
				assertThat(ex.getFutureAppointments()).isEqualTo(3);
				assertThat(ex.getProperties()).containsEntry("futureAppointments", 3L);
				assertThat(ex.getMessage()).contains("3 future appointments").contains("deactivate");
			});
		verifyNoInteractions(employeeCommandRepository);
	}

	@Test
	void unknownForeignOrDeletedEmployeeIsNotFound() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findOfBusiness(BUSINESS_ID, EMPLOYEE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.delete(OWNER_ID, EMPLOYEE_ID)).isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Employee not found");
		verifyNoInteractions(futureAppointmentsQuery, employeeCommandRepository);
	}

	@Test
	void userWithoutBusinessIsRejectedBeforeAnyLookup() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.delete(OWNER_ID, EMPLOYEE_ID)).isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(employeeQueryRepository, futureAppointmentsQuery, employeeCommandRepository);
	}

	private void givenEmployee(EmployeeStatus status) {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findOfBusiness(BUSINESS_ID, EMPLOYEE_ID)).willReturn(Optional.of(new Employee(
				EMPLOYEE_ID, BUSINESS_ID, "Carlos", "Gómez", "+573001234567", "carlos@elcorte.com", status, CREATED,
				CREATED)));
	}

}
