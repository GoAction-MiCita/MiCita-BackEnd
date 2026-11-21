package com.micita.micita_project.core.service.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.service.application.dto.request.UpdateServiceRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;
import com.micita.micita_project.core.service.application.mappers.ServiceMapper;
import com.micita.micita_project.core.service.application.ports.in.AssignServiceEmployeesUseCase;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.DuplicateServiceException;
import com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-10: the data and the employees of a service of the caller's business are replaced; the service itself
 * doesn't count as a duplicate of its own name.
 */
@ExtendWith(MockitoExtension.class)
class UpdateServiceCommandHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID SERVICE_ID = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final Instant CREATED = Instant.parse("2026-10-29T15:00:00Z");

	private static final Instant NOW = Instant.parse("2026-11-02T09:30:00Z");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@Mock
	private ServiceCommandRepositoryPort serviceCommandRepository;

	@Mock
	private AssignServiceEmployeesUseCase assignServiceEmployees;

	private UpdateServiceCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new UpdateServiceCommandHandler(businessQuery, serviceQueryRepository, serviceCommandRepository,
				assignServiceEmployees, Mappers.getMapper(ServiceMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void replacesTheDataAndTheEmployeesKeepingIdAndCreationTime() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(current()));
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(EMPLOYEE_ID)))
			.willReturn(new LinkedHashSet<>(List.of(EMPLOYEE_ID)));

		ServiceResponse response = handler.update(OWNER_ID, SERVICE_ID, new UpdateServiceRequest("  Peinado  ",
				" Con brillo ", 60, new BigDecimal("50000.00"), "INACTIVE", List.of(EMPLOYEE_ID)));

		ArgumentCaptor<Service> saved = ArgumentCaptor.forClass(Service.class);
		verify(serviceCommandRepository).save(saved.capture());
		assertThat(saved.getValue()).isEqualTo(new Service(SERVICE_ID, BUSINESS_ID, "Peinado", "Con brillo", 60,
				new BigDecimal("50000.00"), ServiceStatus.INACTIVE, CREATED, NOW));
		assertThat(response).isEqualTo(new ServiceResponse(SERVICE_ID, "Peinado", "Con brillo", 60,
				new BigDecimal("50000.00"), "INACTIVE", List.of(EMPLOYEE_ID), CREATED));
	}

	@Test
	void optionalFieldsSentBlankAreCleared() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(current()));
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of())).willReturn(Set.of());

		ServiceResponse response = handler.update(OWNER_ID, SERVICE_ID,
				new UpdateServiceRequest("Corte", "   ", null, null, "ACTIVE", List.of()));

		assertThat(response.description()).isNull();
		assertThat(response.durationMinutes()).isNull();
		assertThat(response.price()).isNull();
	}

	@Test
	void emptyEmployeeListRemovesEveryEmployee() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(current()));
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of())).willReturn(Set.of());

		ServiceResponse response = handler.update(OWNER_ID, SERVICE_ID,
				new UpdateServiceRequest("Corte", null, null, null, "ACTIVE", List.of()));

		assertThat(response.employeeIds()).isEmpty();
		verify(assignServiceEmployees).replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of());
	}

	@Test
	void nameUsedByAnotherServiceIsConflictAndNothingChanges() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(current()));
		given(serviceQueryRepository.existsByNameExcept(BUSINESS_ID, "Peinado", SERVICE_ID)).willReturn(true);

		assertThatThrownBy(() -> handler.update(OWNER_ID, SERVICE_ID,
				new UpdateServiceRequest(" Peinado ", null, null, null, "ACTIVE", List.of())))
			.isInstanceOf(DuplicateServiceException.class);
		verify(serviceCommandRepository, never()).save(any());
		verifyNoInteractions(assignServiceEmployees);
	}

	@Test
	void duplicateCheckExcludesTheServiceItself() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(current()));
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of())).willReturn(Set.of());

		handler.update(OWNER_ID, SERVICE_ID, new UpdateServiceRequest("Corte", null, null, null, "ACTIVE", List.of()));

		verify(serviceQueryRepository).existsByNameExcept(BUSINESS_ID, "Corte", SERVICE_ID);
		verify(serviceQueryRepository, never()).existsByName(any(), any());
	}

	@Test
	void unknownForeignOrDeletedServiceIsNotFound() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.update(OWNER_ID, SERVICE_ID,
				new UpdateServiceRequest("Corte", null, null, null, "ACTIVE", List.of())))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Service not found");
		verify(serviceCommandRepository, never()).save(any());
		verifyNoInteractions(assignServiceEmployees);
	}

	@Test
	void rejectedEmployeesPropagateSoTheTransactionRollsTheEditBack() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(current()));
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(EMPLOYEE_ID)))
			.willThrow(new InvalidEmployeeIdsException(List.of(EMPLOYEE_ID)));

		assertThatThrownBy(() -> handler.update(OWNER_ID, SERVICE_ID,
				new UpdateServiceRequest("Corte", null, null, null, "ACTIVE", List.of(EMPLOYEE_ID))))
			.isInstanceOfSatisfying(InvalidEmployeeIdsException.class,
					exception -> assertThat(exception.getRejectedIds()).containsExactly(EMPLOYEE_ID));
	}

	@Test
	void userWithoutBusinessCannotEditServices() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.update(OWNER_ID, SERVICE_ID,
				new UpdateServiceRequest("Corte", null, null, null, "ACTIVE", List.of())))
			.isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(serviceQueryRepository, serviceCommandRepository, assignServiceEmployees);
	}

	private static Service current() {
		return new Service(SERVICE_ID, BUSINESS_ID, "Corte", "Clásico", 45, new BigDecimal("35000.00"),
				ServiceStatus.ACTIVE, CREATED, CREATED);
	}

}
