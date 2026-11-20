package com.micita.micita_project.core.service.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.service.application.dto.request.ReplaceServiceEmployeesRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceEmployeesResponse;
import com.micita.micita_project.core.service.application.ports.in.AssignServiceEmployeesUseCase;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-12: the service must belong to the caller's business, the list replaces the assignment (empty clears
 * it, repeats count once) and employee validation errors propagate.
 */
@ExtendWith(MockitoExtension.class)
class ReplaceServiceEmployeesCommandHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID SERVICE_ID = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final UUID ANA = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID LUIS = UUID.fromString("1b2c3d4e-5f6a-4b7c-8d9e-0f1a2b3c4d5e");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@Mock
	private AssignServiceEmployeesUseCase assignServiceEmployees;

	@InjectMocks
	private ReplaceServiceEmployeesCommandHandler handler;

	@Test
	void replacesTheAssignmentAndReturnsTheEmployees() {
		givenServiceOfTheBusiness();
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(ANA, LUIS)))
			.willReturn(new LinkedHashSet<>(List.of(ANA, LUIS)));

		ServiceEmployeesResponse response = handler.replaceEmployees(OWNER_ID, SERVICE_ID,
				new ReplaceServiceEmployeesRequest(List.of(ANA, LUIS)));

		assertThat(response).isEqualTo(new ServiceEmployeesResponse(SERVICE_ID, List.of(ANA, LUIS)));
	}

	@Test
	void emptyListClearsTheAssignment() {
		givenServiceOfTheBusiness();
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of())).willReturn(Set.of());

		ServiceEmployeesResponse response = handler.replaceEmployees(OWNER_ID, SERVICE_ID,
				new ReplaceServiceEmployeesRequest(List.of()));

		assertThat(response.employeeIds()).isEmpty();
	}

	@Test
	void repeatedIdsAreReportedOnce() {
		givenServiceOfTheBusiness();
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(ANA, ANA)))
			.willReturn(new LinkedHashSet<>(List.of(ANA)));

		ServiceEmployeesResponse response = handler.replaceEmployees(OWNER_ID, SERVICE_ID,
				new ReplaceServiceEmployeesRequest(List.of(ANA, ANA)));

		assertThat(response.employeeIds()).containsExactly(ANA);
	}

	@Test
	void rejectedEmployeesPropagate() {
		givenServiceOfTheBusiness();
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(ANA)))
			.willThrow(new InvalidEmployeeIdsException(List.of(ANA)));

		assertThatThrownBy(() -> handler.replaceEmployees(OWNER_ID, SERVICE_ID,
				new ReplaceServiceEmployeesRequest(List.of(ANA)))).isInstanceOf(InvalidEmployeeIdsException.class);
	}

	@Test
	void serviceOutsideTheBusinessOrDeletedIsNotFoundAndNothingChanges() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.replaceEmployees(OWNER_ID, SERVICE_ID,
				new ReplaceServiceEmployeesRequest(List.of(ANA))))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Service not found");
		verifyNoInteractions(assignServiceEmployees);
	}

	@Test
	void userWithoutBusinessIsRejectedBeforeAnyLookup() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.replaceEmployees(OWNER_ID, SERVICE_ID,
				new ReplaceServiceEmployeesRequest(List.of(ANA)))).isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(serviceQueryRepository, assignServiceEmployees);
	}

	private void givenServiceOfTheBusiness() {
		Instant created = Instant.parse("2026-10-01T10:00:00Z");
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(new Service(
				SERVICE_ID, BUSINESS_ID, "Corte", "Clásico", 45, new BigDecimal("35000.00"), ServiceStatus.ACTIVE, created,
				created)));
	}

}
