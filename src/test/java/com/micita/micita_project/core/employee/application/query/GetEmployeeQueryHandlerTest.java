package com.micita.micita_project.core.employee.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeDetailResponse;
import com.micita.micita_project.core.employee.application.mappers.EmployeeMapper;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-18: the detail carries the employee's non-deleted services, and employees outside the caller's
 * business or deleted are not found.
 */
@ExtendWith(MockitoExtension.class)
class GetEmployeeQueryHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID CUT = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final Instant CREATED = Instant.parse("2026-10-01T10:00:00Z");

	private static final Instant UPDATED = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private EmployeeQueryRepositoryPort employeeQueryRepository;

	@Mock
	private EmployeeServicesQueryPort employeeServicesQuery;

	private GetEmployeeQueryHandler handler;

	@BeforeEach
	void setUp() {
		handler = new GetEmployeeQueryHandler(businessQuery, employeeQueryRepository, employeeServicesQuery,
				Mappers.getMapper(EmployeeMapper.class));
	}

	@Test
	void returnsTheEmployeeWithItsServices() {
		givenEmployee();
		given(employeeServicesQuery.findActiveServicesOf(BUSINESS_ID, List.of(EMPLOYEE_ID)))
			.willReturn(Map.of(EMPLOYEE_ID, List.of(new EmployeeServiceRef(CUT, "Corte"))));

		EmployeeDetailResponse detail = handler.get(OWNER_ID, EMPLOYEE_ID);

		assertThat(detail).isEqualTo(new EmployeeDetailResponse(EMPLOYEE_ID, "Carlos", "Gómez", "+573001234567",
				"carlos@elcorte.com", "INACTIVE", List.of(new EmployeeServiceRef(CUT, "Corte")), CREATED, UPDATED));
	}

	@Test
	void employeeWithoutServicesHasAnEmptyList() {
		givenEmployee();
		given(employeeServicesQuery.findActiveServicesOf(BUSINESS_ID, List.of(EMPLOYEE_ID))).willReturn(Map.of());

		assertThat(handler.get(OWNER_ID, EMPLOYEE_ID).services()).isEmpty();
	}

	@Test
	void employeeOutsideTheBusinessOrDeletedIsNotFound() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findOfBusiness(BUSINESS_ID, EMPLOYEE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.get(OWNER_ID, EMPLOYEE_ID)).isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Employee not found");
		verifyNoInteractions(employeeServicesQuery);
	}

	@Test
	void userWithoutBusinessIsRejectedBeforeAnyLookup() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.get(OWNER_ID, EMPLOYEE_ID)).isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(employeeQueryRepository, employeeServicesQuery);
	}

	private void givenEmployee() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(employeeQueryRepository.findOfBusiness(BUSINESS_ID, EMPLOYEE_ID)).willReturn(Optional.of(new Employee(
				EMPLOYEE_ID, BUSINESS_ID, "Carlos", "Gómez", "+573001234567", "carlos@elcorte.com",
				EmployeeStatus.INACTIVE, CREATED, UPDATED)));
	}

}
