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
import java.util.UUID;

import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.service.application.dto.request.CreateServiceRequest;
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
 * HU-09: a service is created in the caller's business, with optional employees; a name already used by
 * another non-deleted service of the business is a conflict.
 */
@ExtendWith(MockitoExtension.class)
class CreateServiceCommandHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID SERVICE_ID = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final UUID EMPLOYEE_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@Mock
	private ServiceCommandRepositoryPort serviceCommandRepository;

	@Mock
	private AssignServiceEmployeesUseCase assignServiceEmployees;

	private CreateServiceCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new CreateServiceCommandHandler(businessQuery, serviceQueryRepository, serviceCommandRepository,
				assignServiceEmployees, Mappers.getMapper(ServiceMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void createsServiceInTheCallersBusinessWithItsEmployees() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(EMPLOYEE_ID)))
			.willReturn(new LinkedHashSet<>(List.of(EMPLOYEE_ID)));

		ServiceResponse response = handler.create(OWNER_ID, new CreateServiceRequest("  Corte  ", " Clásico ", 45,
				new BigDecimal("35000.00"), "ACTIVE", List.of(EMPLOYEE_ID)));

		ArgumentCaptor<Service> saved = ArgumentCaptor.forClass(Service.class);
		verify(serviceCommandRepository).save(saved.capture());
		assertThat(saved.getValue().businessId()).isEqualTo(BUSINESS_ID);
		assertThat(saved.getValue().name()).isEqualTo("Corte");
		assertThat(saved.getValue().description()).isEqualTo("Clásico");
		assertThat(saved.getValue().status()).isEqualTo(ServiceStatus.ACTIVE);
		assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
		assertThat(saved.getValue().updatedAt()).isEqualTo(NOW);
		assertThat(response).isEqualTo(new ServiceResponse(SERVICE_ID, "Corte", "Clásico", 45,
				new BigDecimal("35000.00"), "ACTIVE", List.of(EMPLOYEE_ID), NOW));
	}

	@Test
	void optionalFieldsAreStoredAsAbsentAndNoEmployeesAreAssigned() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));

		ServiceResponse response = handler.create(OWNER_ID,
				new CreateServiceRequest("Corte", "   ", null, null, "INACTIVE", null));

		assertThat(response.description()).isNull();
		assertThat(response.durationMinutes()).isNull();
		assertThat(response.price()).isNull();
		assertThat(response.status()).isEqualTo("INACTIVE");
		assertThat(response.employeeIds()).isEmpty();
		verifyNoInteractions(assignServiceEmployees);
	}

	@Test
	void emptyEmployeeListAssignsNothing() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));

		handler.create(OWNER_ID, new CreateServiceRequest("Corte", null, null, null, "ACTIVE", List.of()));

		verifyNoInteractions(assignServiceEmployees);
	}

	@Test
	void nameAlreadyUsedInTheBusinessIsConflictAndNothingIsSaved() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.existsByName(BUSINESS_ID, "Corte")).willReturn(true);

		assertThatThrownBy(() -> handler.create(OWNER_ID,
				new CreateServiceRequest(" Corte ", null, null, null, "ACTIVE", null)))
			.isInstanceOf(DuplicateServiceException.class)
			.hasMessage("Another service of the business already uses this name");
		verify(serviceCommandRepository, never()).save(any());
		verifyNoInteractions(assignServiceEmployees);
	}

	@Test
	void rejectedEmployeesPropagateSoTheTransactionRollsTheServiceBack() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));
		given(assignServiceEmployees.replaceEmployeesOfService(BUSINESS_ID, SERVICE_ID, List.of(EMPLOYEE_ID)))
			.willThrow(new InvalidEmployeeIdsException(List.of(EMPLOYEE_ID)));

		assertThatThrownBy(() -> handler.create(OWNER_ID,
				new CreateServiceRequest("Corte", null, null, null, "ACTIVE", List.of(EMPLOYEE_ID))))
			.isInstanceOfSatisfying(InvalidEmployeeIdsException.class,
					exception -> assertThat(exception.getRejectedIds()).containsExactly(EMPLOYEE_ID));
	}

	@Test
	void userWithoutBusinessCannotCreateServices() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler
			.create(OWNER_ID, new CreateServiceRequest("Corte", null, null, null, "ACTIVE", null)))
			.isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(serviceQueryRepository, serviceCommandRepository, assignServiceEmployees);
	}

	private static Service withId(Service service) {
		return new Service(SERVICE_ID, service.businessId(), service.name(), service.description(),
				service.durationMinutes(), service.price(), service.status(), service.createdAt(), service.updatedAt());
	}

}
