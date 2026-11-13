package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.mappers.ServiceBDMapper;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ServiceQueryJpaAdapterTest {

	@Mock
	private ServiceJpaRepository serviceJpaRepository;

	@Mock
	private ServiceBDMapper serviceBDMapper;

	@InjectMocks
	private ServiceQueryJpaAdapter adapter;

	@Test
	void pageOfTheBusinessExcludesDeletedAndSortsByNameIgnoringCase() {
		UUID businessId = UUID.randomUUID();
		ServiceEntity entity = new ServiceEntity();
		Service service = new Service(UUID.randomUUID(), businessId, "Corte", null, null, null, ServiceStatus.ACTIVE,
				Instant.now(), Instant.now());
		given(serviceJpaRepository.findByBusinessIdAndStatusNot(eq(businessId), eq(ServiceStatus.DELETED), any()))
			.willReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 5), 11));
		given(serviceBDMapper.toDomain(entity)).willReturn(service);

		PageResponse<Service> page = adapter.findPageOfBusiness(businessId, 1, 5);

		assertThat(page.content()).containsExactly(service);
		assertThat(page.page()).isEqualTo(1);
		assertThat(page.size()).isEqualTo(5);
		assertThat(page.totalElements()).isEqualTo(11);
		assertThat(page.totalPages()).isEqualTo(3);
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(serviceJpaRepository).findByBusinessIdAndStatusNot(eq(businessId), eq(ServiceStatus.DELETED),
				pageable.capture());
		assertThat(pageable.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
		assertThat(pageable.getValue().getSort().getOrderFor("name").isIgnoreCase()).isTrue();
		assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
	}

	@Test
	void assignableIdsExcludeDeletedServices() {
		UUID businessId = UUID.randomUUID();
		UUID serviceId = UUID.randomUUID();
		given(serviceJpaRepository.findIdsOfBusiness(businessId, List.of(serviceId), ServiceStatus.DELETED))
			.willReturn(Set.of(serviceId));

		assertThat(adapter.findAssignableIds(businessId, List.of(serviceId))).containsExactly(serviceId);
	}

	@Test
	void nameLookupSkipsDeletedServices() {
		UUID businessId = UUID.randomUUID();
		given(serviceJpaRepository.existsByName(businessId, "Corte", ServiceStatus.DELETED)).willReturn(true);

		assertThat(adapter.existsByName(businessId, "Corte")).isTrue();
		assertThat(adapter.existsByName(businessId, "Otro")).isFalse();
	}

}
