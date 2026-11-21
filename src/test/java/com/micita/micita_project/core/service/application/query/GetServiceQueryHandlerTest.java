package com.micita.micita_project.core.service.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import com.micita.micita_project.core.service.application.dto.response.ServiceDetailResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceEmployeeResponse;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.EmployeeQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceEmployeeQueryRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceEmployee;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-10: the detail of a service with its non-deleted employees, by name; 404 when it isn't visible.
 */
@ExtendWith(MockitoExtension.class)
class GetServiceQueryHandlerTest {

	private static final UUID OWNER_ID = UUID.randomUUID();

	private static final UUID BUSINESS_ID = UUID.randomUUID();

	private static final UUID SERVICE_ID = UUID.randomUUID();

	private static final Instant CREATED = Instant.parse("2026-10-29T15:00:00Z");

	private static final Instant UPDATED = Instant.parse("2026-11-02T09:30:00Z");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@Mock
	private ServiceEmployeeQueryRepositoryPort serviceEmployeeQueryRepository;

	@Mock
	private EmployeeQueryPort employeeQuery;

	@InjectMocks
	private GetServiceQueryHandler handler;

	@Test
	void returnsTheServiceWithItsEmployeesByName() {
		UUID ana = UUID.randomUUID();
		UUID luis = UUID.randomUUID();
		UUID deleted = UUID.randomUUID();
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(service()));
		given(serviceEmployeeQueryRepository.findEmployeeIdsByService(List.of(SERVICE_ID)))
			.willReturn(Map.of(SERVICE_ID, Set.of(ana, luis, deleted)));
		// The employee module leaves the deleted one out.
		given(employeeQuery.findEmployees(BUSINESS_ID, Set.of(ana, luis, deleted)))
			.willReturn(List.of(new ServiceEmployee(luis, "Luis", "Mora"), new ServiceEmployee(ana, "Ana", "Ruiz")));

		ServiceDetailResponse response = handler.get(OWNER_ID, SERVICE_ID);

		assertThat(response).isEqualTo(new ServiceDetailResponse(SERVICE_ID, "Corte", "Clásico", 45,
				new BigDecimal("35000.00"), "ACTIVE",
				List.of(new ServiceEmployeeResponse(ana, "Ana", "Ruiz", null),
						new ServiceEmployeeResponse(luis, "Luis", "Mora", null)),
				CREATED, UPDATED));
	}

	@Test
	void serviceWithoutEmployeesHasAnEmptyList() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.of(service()));
		given(serviceEmployeeQueryRepository.findEmployeeIdsByService(List.of(SERVICE_ID))).willReturn(Map.of());

		assertThat(handler.get(OWNER_ID, SERVICE_ID).employees()).isEmpty();
		verifyNoInteractions(employeeQuery);
	}

	@Test
	void unknownForeignOrDeletedServiceIsNotFound() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findOfBusiness(BUSINESS_ID, SERVICE_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.get(OWNER_ID, SERVICE_ID)).isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Service not found");
		verifyNoInteractions(serviceEmployeeQueryRepository, employeeQuery);
	}

	@Test
	void userWithoutBusinessIsRejected() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new BusinessRequiredException());

		assertThatThrownBy(() -> handler.get(OWNER_ID, SERVICE_ID)).isInstanceOf(BusinessRequiredException.class);
		verifyNoInteractions(serviceQueryRepository);
	}

	private static Service service() {
		return new Service(SERVICE_ID, BUSINESS_ID, "Corte", "Clásico", 45, new BigDecimal("35000.00"),
				ServiceStatus.ACTIVE, CREATED, UPDATED);
	}

}
