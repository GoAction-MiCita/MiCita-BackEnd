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
import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.service.application.dto.request.ChangeServiceStatusRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceStatusResponse;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-11: ACTIVE and INACTIVE switch both ways, the current status is a no-op, and a service outside the
 * caller's business (or deleted) is not found.
 */
@ExtendWith(MockitoExtension.class)
class ChangeServiceStatusCommandHandlerTest {

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

	private ChangeServiceStatusCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new ChangeServiceStatusCommandHandler(businessQuery, serviceQueryRepository, serviceCommandRepository,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void deactivatesAnActiveServiceKeepingEverythingElse() {
		givenService(ServiceStatus.ACTIVE);
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		ServiceStatusResponse response = handler.changeStatus(OWNER_ID, SERVICE_ID,
				new ChangeServiceStatusRequest("INACTIVE"));

		ArgumentCaptor<Service> saved = ArgumentCaptor.forClass(Service.class);
		verify(serviceCommandRepository).save(saved.capture());
		assertThat(saved.getValue()).isEqualTo(new Service(SERVICE_ID, BUSINESS_ID, "Corte", "Clásico", 45,
				new BigDecimal("35000.00"), ServiceStatus.INACTIVE, CREATED, NOW));
		assertThat(response).isEqualTo(new ServiceStatusResponse(SERVICE_ID, "INACTIVE"));
	}

	@Test
	void activatesAnInactiveService() {
		givenService(ServiceStatus.INACTIVE);
		given(serviceCommandRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

		ServiceStatusResponse response = handler.changeStatus(OWNER_ID, SERVICE_ID,
				new ChangeServiceStatusRequest("ACTIVE"));

		assertThat(response).isEqualTo(new ServiceStatusResponse(SERVICE_ID, "ACTIVE"));
	}

	@Test
	void sameStatusChangesNothing() {
		givenService(ServiceStatus.INACTIVE);

		ServiceStatusResponse response = handler.changeStatus(OWNER_ID, SERVICE_ID,
				new ChangeServiceStatusRequest("INACTIVE"));

		assertThat(response).isEqualTo(new ServiceStatusResponse(SERVICE_ID, "INACTIVE"));
		verify(serviceCommandRepository, never()).save(any());
	}

	@Test
	void serviceOutsideTheBusinessOrDeletedIsNotFound() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.changeStatus(OWNER_ID, SERVICE_ID, new ChangeServiceStatusRequest("ACTIVE")))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Service not found");
		verifyNoInteractions(serviceCommandRepository);
	}

	@Test
	void userWithoutBusinessIsRejectedBeforeAnyLookup() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.changeStatus(OWNER_ID, SERVICE_ID, new ChangeServiceStatusRequest("ACTIVE")))
			.isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(serviceQueryRepository, serviceCommandRepository);
	}

	private void givenService(ServiceStatus status) {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(new Service(
				SERVICE_ID, BUSINESS_ID, "Corte", "Clásico", 45, new BigDecimal("35000.00"), status, CREATED, CREATED)));
	}

}
