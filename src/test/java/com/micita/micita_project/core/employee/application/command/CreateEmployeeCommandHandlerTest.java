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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.employee.application.dto.request.CreateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.mappers.EmployeeMapper;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeServicesCommandPort;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.domain.exceptions.DuplicateEmployeeException;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.service.domain.exceptions.InvalidServiceIdsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-17: an employee is created in the caller's business, with optional services; email or phone used
 * by another employee of the business is a conflict.
 */
@ExtendWith(MockitoExtension.class)
class CreateEmployeeCommandHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID CUT = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private EmployeeQueryRepositoryPort employeeQueryRepository;

	@Mock
	private EmployeeCommandRepositoryPort employeeCommandRepository;

	@Mock
	private EmployeeServicesCommandPort employeeServices;

	private CreateEmployeeCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new CreateEmployeeCommandHandler(businessQuery, employeeQueryRepository, employeeCommandRepository,
				employeeServices, Mappers.getMapper(EmployeeMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void createsEmployeeInTheCallersBusinessWithItsServices() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));
		given(employeeServices.replaceServices(BUSINESS_ID, EMPLOYEE_ID, List.of(CUT, CUT)))
			.willReturn(new LinkedHashSet<>(List.of(CUT)));

		EmployeeResponse response = handler.create(OWNER_ID, new CreateEmployeeRequest(" Carlos ", " Gómez ",
				" +573001234567 ", " Carlos@ElCorte.com ", "ACTIVE", List.of(CUT, CUT)));

		ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
		verify(employeeCommandRepository).save(saved.capture());
		assertThat(saved.getValue()).isEqualTo(new Employee(null, BUSINESS_ID, "Carlos", "Gómez", "+573001234567",
				"carlos@elcorte.com", EmployeeStatus.ACTIVE, NOW, NOW));
		assertThat(response).isEqualTo(new EmployeeResponse(EMPLOYEE_ID, "Carlos", "Gómez", "+573001234567",
				"carlos@elcorte.com", "ACTIVE", List.of(CUT), NOW));
	}

	@Test
	void optionalFieldsMayBeMissingOrBlank() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));

		EmployeeResponse response = handler.create(OWNER_ID,
				new CreateEmployeeRequest("Carlos", "Gómez", " ", null, "INACTIVE", List.of()));

		assertThat(response.phone()).isNull();
		assertThat(response.email()).isNull();
		assertThat(response.status()).isEqualTo("INACTIVE");
		assertThat(response.serviceIds()).isEmpty();
		verifyNoInteractions(employeeQueryRepository, employeeServices);
	}

	@Test
	void emailUsedByAnotherEmployeeIsConflict() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.existsByEmail(BUSINESS_ID, "carlos@elcorte.com")).willReturn(true);

		assertThatThrownBy(() -> handler.create(OWNER_ID,
				new CreateEmployeeRequest("Carlos", "Gómez", null, "CARLOS@elcorte.com", "ACTIVE", null)))
			.isInstanceOf(DuplicateEmployeeException.class)
			.hasMessage("Another employee of the business already uses this email");
		verify(employeeCommandRepository, never()).save(any());
	}

	@Test
	void phoneUsedByAnotherEmployeeIsConflict() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.existsByEmail(BUSINESS_ID, "carlos@elcorte.com")).willReturn(false);
		given(employeeQueryRepository.existsByPhone(BUSINESS_ID, "+573001234567")).willReturn(true);

		assertThatThrownBy(() -> handler.create(OWNER_ID, new CreateEmployeeRequest("Carlos", "Gómez",
				"+573001234567", "carlos@elcorte.com", "ACTIVE", null)))
			.isInstanceOf(DuplicateEmployeeException.class)
			.hasMessage("Another employee of the business already uses this phone");
		verify(employeeCommandRepository, never()).save(any());
	}

	@Test
	void rejectedServicesPropagateSoTheTransactionRollsBack() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));
		given(employeeServices.replaceServices(BUSINESS_ID, EMPLOYEE_ID, List.of(CUT)))
			.willThrow(new InvalidServiceIdsException(List.of(CUT)));

		assertThatThrownBy(() -> handler.create(OWNER_ID,
				new CreateEmployeeRequest("Carlos", "Gómez", null, null, "ACTIVE", List.of(CUT))))
			.isInstanceOf(InvalidServiceIdsException.class);
	}

	@Test
	void userWithoutBusinessCannotCreateEmployees() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.create(OWNER_ID,
				new CreateEmployeeRequest("Carlos", "Gómez", null, null, "ACTIVE", null)))
			.isInstanceOf(BusinessRequiredException.class);
		verify(employeeCommandRepository, never()).save(any());
	}

	private static Employee withId(Employee employee) {
		return new Employee(EMPLOYEE_ID, employee.businessId(), employee.firstName(), employee.lastName(),
				employee.phone(), employee.email(), employee.status(), employee.createdAt(), employee.updatedAt());
	}

}
