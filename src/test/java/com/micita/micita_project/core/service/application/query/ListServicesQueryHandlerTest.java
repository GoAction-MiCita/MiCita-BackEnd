package com.micita.micita_project.core.service.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidPageRequestException;
import com.micita.micita_project.common.shared.exception.ForbiddenException;
import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceListItemResponse;
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
 * HU-08: the page of services with their employees, loaded in batch.
 */
@ExtendWith(MockitoExtension.class)
class ListServicesQueryHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	@Mock
	private BusinessQueryPort businessQuery;

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@Mock
	private ServiceEmployeeQueryRepositoryPort serviceEmployeeQueryRepository;

	@Mock
	private EmployeeQueryPort employeeQuery;

	@InjectMocks
	private ListServicesQueryHandler handler;

	@Test
	void listsTheServicesOfTheCallersBusinessWithTheirEmployees() {
		UUID corte = UUID.randomUUID();
		UUID tinte = UUID.randomUUID();
		UUID ana = UUID.randomUUID();
		UUID luis = UUID.randomUUID();
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findPageOfBusiness(BUSINESS_ID, 0, 10))
			.willReturn(new PageResponse<>(List.of(service(corte, "Corte", ServiceStatus.ACTIVE),
					service(tinte, "Tinte", ServiceStatus.INACTIVE)), 0, 10, 2, 1));
		given(serviceEmployeeQueryRepository.findEmployeeIdsByService(List.of(corte, tinte)))
			.willReturn(Map.of(corte, Set.of(luis, ana)));
		given(employeeQuery.findEmployees(any(), any()))
			.willReturn(List.of(new ServiceEmployee(luis, "Luis", "Mora"), new ServiceEmployee(ana, "Ana", "Ruiz")));

		PageResponse<ServiceListItemResponse> page = handler.list(OWNER_ID, 0, 10);

		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(10);
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(page.content()).hasSize(2);
		ServiceListItemResponse first = page.content().get(0);
		assertThat(first.id()).isEqualTo(corte);
		assertThat(first.name()).isEqualTo("Corte");
		assertThat(first.description()).isEqualTo("Clásico");
		assertThat(first.durationMinutes()).isEqualTo(45);
		assertThat(first.price()).isEqualByComparingTo("35000.00");
		assertThat(first.status()).isEqualTo("ACTIVE");
		assertThat(first.employees()).extracting("id", "firstName", "lastName", "photoUrl")
			.containsExactly(org.assertj.core.groups.Tuple.tuple(ana, "Ana", "Ruiz", null),
					org.assertj.core.groups.Tuple.tuple(luis, "Luis", "Mora", null));
		assertThat(page.content().get(1).status()).isEqualTo("INACTIVE");
		assertThat(page.content().get(1).employees()).isEmpty();
	}

	@Test
	void employeesAreLoadedOncePerPage() {
		UUID corte = UUID.randomUUID();
		UUID tinte = UUID.randomUUID();
		UUID ana = UUID.randomUUID();
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findPageOfBusiness(BUSINESS_ID, 0, 10))
			.willReturn(new PageResponse<>(List.of(service(corte, "Corte", ServiceStatus.ACTIVE),
					service(tinte, "Tinte", ServiceStatus.ACTIVE)), 0, 10, 2, 1));
		given(serviceEmployeeQueryRepository.findEmployeeIdsByService(List.of(corte, tinte)))
			.willReturn(Map.of(corte, Set.of(ana), tinte, Set.of(ana)));
		given(employeeQuery.findEmployees(BUSINESS_ID, Set.of(ana)))
			.willReturn(List.of(new ServiceEmployee(ana, "Ana", "Ruiz")));

		PageResponse<ServiceListItemResponse> page = handler.list(OWNER_ID, 0, 10);

		assertThat(page.content()).allSatisfy(item -> assertThat(item.employees()).hasSize(1));
		verify(serviceEmployeeQueryRepository).findEmployeeIdsByService(any(Collection.class));
		verify(employeeQuery).findEmployees(any(), any());
	}

	@Test
	void deletedOrUnknownEmployeesAreLeftOut() {
		UUID corte = UUID.randomUUID();
		UUID deleted = UUID.randomUUID();
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findPageOfBusiness(BUSINESS_ID, 0, 10))
			.willReturn(new PageResponse<>(List.of(service(corte, "Corte", ServiceStatus.ACTIVE)), 0, 10, 1, 1));
		given(serviceEmployeeQueryRepository.findEmployeeIdsByService(List.of(corte)))
			.willReturn(Map.of(corte, Set.of(deleted)));
		given(employeeQuery.findEmployees(BUSINESS_ID, Set.of(deleted))).willReturn(List.of());

		assertThat(handler.list(OWNER_ID, 0, 10).content().get(0).employees()).isEmpty();
	}

	@Test
	void businessWithoutServicesGetsAnEmptyPage() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findPageOfBusiness(BUSINESS_ID, 0, 10))
			.willReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));

		PageResponse<ServiceListItemResponse> page = handler.list(OWNER_ID, 0, 10);

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isZero();
		verifyNoInteractions(serviceEmployeeQueryRepository, employeeQuery);
	}

	@Test
	void serviceWithoutEmployeesNeedsNoEmployeeLookup() {
		UUID corte = UUID.randomUUID();
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willReturn(BUSINESS_ID);
		given(serviceQueryRepository.findPageOfBusiness(BUSINESS_ID, 0, 10))
			.willReturn(new PageResponse<>(List.of(service(corte, "Corte", ServiceStatus.ACTIVE)), 0, 10, 1, 1));
		given(serviceEmployeeQueryRepository.findEmployeeIdsByService(List.of(corte))).willReturn(Map.of());

		assertThat(handler.list(OWNER_ID, 0, 10).content().get(0).employees()).isEmpty();
		verifyNoInteractions(employeeQuery);
	}

	@Test
	void invalidPagingIsRejectedWithAnErrorPerField() {
		assertThatThrownBy(() -> handler.list(OWNER_ID, -1, 51)).isInstanceOfSatisfying(
				InvalidPageRequestException.class,
				ex -> assertThat(ex.getErrors()).containsKeys("page", "size"));
		assertThatThrownBy(() -> handler.list(OWNER_ID, 0, 0)).isInstanceOf(InvalidPageRequestException.class);
		verifyNoInteractions(businessQuery, serviceQueryRepository);
	}

	@Test
	void userWithoutBusinessIsRejected() {
		given(businessQuery.requireBusinessIdOf(OWNER_ID)).willThrow(new ForbiddenException("BUSINESS_REQUIRED", "x"));

		assertThatThrownBy(() -> handler.list(OWNER_ID, 0, 10)).isInstanceOf(ForbiddenException.class);
		verify(serviceQueryRepository, never()).findPageOfBusiness(any(), org.mockito.ArgumentMatchers.anyInt(),
				org.mockito.ArgumentMatchers.anyInt());
	}

	private static Service service(UUID id, String name, ServiceStatus status) {
		return new Service(id, BUSINESS_ID, name, "Clásico", 45, new BigDecimal("35000.00"), status,
				Instant.parse("2026-10-29T15:00:00Z"), Instant.parse("2026-10-29T15:00:00Z"));
	}

}
