package com.micita.micita_project.core.service.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.FutureAppointmentsQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.ServiceHasFutureAppointmentsException;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-14: soft delete keeping the row, 409 with the count when there are future appointments, and 404
 * for unknown, foreign or already deleted services.
 */
@ExtendWith(MockitoExtension.class)
class DeleteServiceCommandHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID SERVICE_ID = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final Instant CREATED = Instant.parse("2026-10-01T10:00:00Z");

	private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@Mock
	private ServiceCommandRepositoryPort serviceCommandRepository;

	@Mock
	private FutureAppointmentsQueryPort futureAppointmentsQuery;

	private DeleteServiceCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new DeleteServiceCommandHandler(businessQuery, serviceQueryRepository, serviceCommandRepository,
				futureAppointmentsQuery, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void marksTheServiceDeletedKeepingEverythingElse() {
		givenService(ServiceStatus.INACTIVE);
		given(futureAppointmentsQuery.countFutureAppointmentsOf(BUSINESS_ID, SERVICE_ID, NOW)).willReturn(0L);

		handler.delete(OWNER_ID, SERVICE_ID);

		ArgumentCaptor<Service> saved = ArgumentCaptor.forClass(Service.class);
		verify(serviceCommandRepository).save(saved.capture());
		assertThat(saved.getValue()).isEqualTo(new Service(SERVICE_ID, BUSINESS_ID, "Corte", "Clásico", 45,
				new BigDecimal("35000.00"), ServiceStatus.DELETED, CREATED, NOW));
	}

	@Test
	void serviceWithFutureAppointmentsIsConflictWithTheCount() {
		givenService(ServiceStatus.ACTIVE);
		given(futureAppointmentsQuery.countFutureAppointmentsOf(BUSINESS_ID, SERVICE_ID, NOW)).willReturn(3L);

		assertThatThrownBy(() -> handler.delete(OWNER_ID, SERVICE_ID))
			.isInstanceOfSatisfying(ServiceHasFutureAppointmentsException.class, ex -> {
				assertThat(ex.getFutureAppointments()).isEqualTo(3);
				assertThat(ex.getProperties()).containsEntry("futureAppointments", 3L);
				assertThat(ex.getMessage()).contains("3 future appointments").contains("deactivate");
			});
		verifyNoInteractions(serviceCommandRepository);
	}

	@Test
	void unknownForeignOrDeletedServiceIsNotFound() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.delete(OWNER_ID, SERVICE_ID)).isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Service not found");
		verifyNoInteractions(futureAppointmentsQuery, serviceCommandRepository);
	}

	@Test
	void userWithoutBusinessIsRejectedBeforeAnyLookup() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.delete(OWNER_ID, SERVICE_ID)).isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(serviceQueryRepository, futureAppointmentsQuery, serviceCommandRepository);
	}

	private void givenService(ServiceStatus status) {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(new Service(
				SERVICE_ID, BUSINESS_ID, "Corte", "Clásico", 45, new BigDecimal("35000.00"), status, CREATED,
				CREATED)));
	}

}
