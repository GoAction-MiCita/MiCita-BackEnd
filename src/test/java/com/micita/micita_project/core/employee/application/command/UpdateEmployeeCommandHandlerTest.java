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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.employee.application.dto.request.UpdateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.mappers.EmployeeMapper;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeServicesCommandPort;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.exceptions.DuplicateEmployeeException;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
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
 * HU-18: the data is replaced (status included) keeping id and creation time, the employee's own email
 * and phone don't clash, {@code serviceIds} replaces the whole assignment when sent, and unknown or
 * foreign employees are not found.
 */
@ExtendWith(MockitoExtension.class)
class UpdateEmployeeCommandHandlerTest {

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
	private EmployeeServicesCommandPort employeeServices;

	@Mock
	private EmployeeServicesQueryPort employeeServicesQuery;

	private UpdateEmployeeCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new UpdateEmployeeCommandHandler(businessQuery, employeeQueryRepository, employeeCommandRepository,
				employeeServices, employeeServicesQuery, Mappers.getMapper(EmployeeMapper.class),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void replacesDataAndServicesKeepingIdAndCreationTime() {
		givenEmployee();
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(employeeServices.replaceServices(BUSINESS_ID, EMPLOYEE_ID, List.of(BEARD)))
			.willReturn(new LinkedHashSet<>(List.of(BEARD)));

		EmployeeResponse response = handler.update(OWNER_ID, EMPLOYEE_ID, new UpdateEmployeeRequest("  Andrés ",
				" Ruiz ", " +573109876543 ", " Andres@ElCorte.com ", "INACTIVE", List.of(BEARD)));

		ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
		verify(employeeCommandRepository).save(saved.capture());
		assertThat(saved.getValue()).isEqualTo(new Employee(EMPLOYEE_ID, BUSINESS_ID, "Andrés", "Ruiz",
				"+573109876543", "andres@elcorte.com", EmployeeStatus.INACTIVE, CREATED, NOW));
		assertThat(response.id()).isEqualTo(EMPLOYEE_ID);
		assertThat(response.status()).isEqualTo("INACTIVE");
		assertThat(response.createdAt()).isEqualTo(CREATED);
		assertThat(response.serviceIds()).containsExactly(BEARD);
	}

	@Test
	void duplicateChecksExcludeTheEmployeeItself() {
		givenEmployee();
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(employeeServices.replaceServices(BUSINESS_ID, EMPLOYEE_ID, List.of())).willReturn(Set.of());

		handler.update(OWNER_ID, EMPLOYEE_ID, request("+573001234567", "carlos@elcorte.com", List.of()));

		verify(employeeQueryRepository).existsByEmailExcept(BUSINESS_ID, "carlos@elcorte.com", EMPLOYEE_ID);
		verify(employeeQueryRepository).existsByPhoneExcept(BUSINESS_ID, "+573001234567", EMPLOYEE_ID);
		verify(employeeQueryRepository, never()).existsByEmail(any(), any());
		verify(employeeQueryRepository, never()).existsByPhone(any(), any());
	}

	@Test
	void emailOfAnotherEmployeeIsConflictAndNothingIsSaved() {
		givenEmployee();
		given(employeeQueryRepository.existsByEmailExcept(BUSINESS_ID, "ana@elcorte.com", EMPLOYEE_ID))
			.willReturn(true);

		assertThatThrownBy(() -> handler.update(OWNER_ID, EMPLOYEE_ID, request(null, "ana@elcorte.com", null)))
			.isInstanceOf(DuplicateEmployeeException.class)
			.hasMessage("Another employee of the business already uses this email");
		verifyNoInteractions(employeeCommandRepository, employeeServices);
	}

	@Test
	void phoneOfAnotherEmployeeIsConflictAndNothingIsSaved() {
		givenEmployee();
		given(employeeQueryRepository.existsByPhoneExcept(BUSINESS_ID, "+573109876543", EMPLOYEE_ID)).willReturn(true);

		assertThatThrownBy(() -> handler.update(OWNER_ID, EMPLOYEE_ID, request("+573109876543", null, null)))
			.isInstanceOf(DuplicateEmployeeException.class)
			.hasMessage("Another employee of the business already uses this phone");
		verifyNoInteractions(employeeCommandRepository, employeeServices);
	}

	@Test
	void blankOptionalFieldsAreClearedWithoutDuplicateChecks() {
		givenEmployee();
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		EmployeeResponse response = handler.update(OWNER_ID, EMPLOYEE_ID, request(" ", "", null));

		assertThat(response.phone()).isNull();
		assertThat(response.email()).isNull();
		verify(employeeQueryRepository, never()).existsByEmailExcept(any(), any(), any());
		verify(employeeQueryRepository, never()).existsByPhoneExcept(any(), any(), any());
	}

	@Test
	void emptyServiceListRemovesEveryService() {
		givenEmployee();
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(employeeServices.replaceServices(BUSINESS_ID, EMPLOYEE_ID, List.of())).willReturn(Set.of());

		EmployeeResponse response = handler.update(OWNER_ID, EMPLOYEE_ID, request(null, null, List.of()));

		verify(employeeServices).replaceServices(BUSINESS_ID, EMPLOYEE_ID, List.of());
		assertThat(response.serviceIds()).isEmpty();
		verifyNoInteractions(employeeServicesQuery);
	}

	@Test
	void missingServiceListKeepsTheCurrentServices() {
		givenEmployee();
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(employeeServicesQuery.findActiveServicesOf(BUSINESS_ID, List.of(EMPLOYEE_ID)))
			.willReturn(Map.of(EMPLOYEE_ID, List.of(new EmployeeServiceRef(BEARD, "Barba"),
					new EmployeeServiceRef(CUT, "Corte"))));

		EmployeeResponse response = handler.update(OWNER_ID, EMPLOYEE_ID, request(null, null, null));

		verifyNoInteractions(employeeServices);
		assertThat(response.serviceIds()).containsExactly(BEARD, CUT);
	}

	@Test
	void unavailableServicesPropagateSoTheTransactionRollsBack() {
		givenEmployee();
		given(employeeCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(employeeServices.replaceServices(BUSINESS_ID, EMPLOYEE_ID, List.of(CUT)))
			.willThrow(new InvalidServiceIdsException(List.of(CUT)));

		assertThatThrownBy(() -> handler.update(OWNER_ID, EMPLOYEE_ID, request(null, null, List.of(CUT))))
			.isInstanceOf(InvalidServiceIdsException.class);
	}

	@Test
	void employeeOutsideTheBusinessOrDeletedIsNotFound() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findOfBusiness(BUSINESS_ID, EMPLOYEE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.update(OWNER_ID, EMPLOYEE_ID, request(null, "ana@elcorte.com", List.of(CUT))))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Employee not found");
		verify(employeeQueryRepository, never()).existsByEmailExcept(any(), any(), any());
		verifyNoInteractions(employeeCommandRepository, employeeServices, employeeServicesQuery);
	}

	@Test
	void userWithoutBusinessIsRejectedBeforeAnyLookup() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.update(OWNER_ID, EMPLOYEE_ID, request(null, null, null)))
			.isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(employeeQueryRepository, employeeCommandRepository, employeeServices,
				employeeServicesQuery);
	}

	private static UpdateEmployeeRequest request(String phone, String email, List<UUID> serviceIds) {
		return new UpdateEmployeeRequest("Carlos", "Gómez", phone, email, "ACTIVE", serviceIds);
	}

	private void givenEmployee() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findOfBusiness(BUSINESS_ID, EMPLOYEE_ID)).willReturn(Optional.of(new Employee(
				EMPLOYEE_ID, BUSINESS_ID, "Carlos", "Gómez", "+573001234567", "carlos@elcorte.com",
				EmployeeStatus.ACTIVE, CREATED, CREATED)));
	}

}
