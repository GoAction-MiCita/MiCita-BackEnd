package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceQueryJpaAdapterTest {

	@Mock
	private ServiceJpaRepository serviceJpaRepository;

	@InjectMocks
	private ServiceQueryJpaAdapter adapter;

	@Test
	void assignableIdsExcludeDeletedServices() {
		UUID businessId = UUID.randomUUID();
		UUID serviceId = UUID.randomUUID();
		given(serviceJpaRepository.findIdsOfBusiness(businessId, List.of(serviceId), ServiceStatus.DELETED))
			.willReturn(Set.of(serviceId));

		assertThat(adapter.findAssignableIds(businessId, List.of(serviceId))).containsExactly(serviceId);
	}

}
