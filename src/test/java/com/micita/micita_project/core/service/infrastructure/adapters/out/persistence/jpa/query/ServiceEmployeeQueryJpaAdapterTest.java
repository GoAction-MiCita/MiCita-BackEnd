package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEmployeeEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceEmployeeJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceEmployeeQueryJpaAdapterTest {

	@Mock
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@InjectMocks
	private ServiceEmployeeQueryJpaAdapter adapter;

	@Test
	void serviceIdsAssignedToAnyOfTheEmployeesComeFromOneLookup() {
		UUID ana = UUID.randomUUID();
		UUID luis = UUID.randomUUID();
		UUID corte = UUID.randomUUID();
		given(serviceEmployeeJpaRepository.findServiceIdsAssignedTo(List.of(ana, luis))).willReturn(Set.of(corte));

		assertThat(adapter.findServiceIdsAssignedTo(List.of(ana, luis))).containsExactly(corte);
	}

	@Test
	void employeeIdsAreGroupedByServiceFromOneLookup() {
		UUID corte = UUID.randomUUID();
		UUID tinte = UUID.randomUUID();
		UUID ana = UUID.randomUUID();
		UUID luis = UUID.randomUUID();
		given(serviceEmployeeJpaRepository.findByServiceIdIn(List.of(corte, tinte)))
			.willReturn(List.of(new ServiceEmployeeEntity(corte, ana), new ServiceEmployeeEntity(corte, luis),
					new ServiceEmployeeEntity(tinte, ana)));

		Map<UUID, Set<UUID>> byService = adapter.findEmployeeIdsByService(List.of(corte, tinte));

		assertThat(byService).containsOnlyKeys(corte, tinte);
		assertThat(byService.get(corte)).containsExactlyInAnyOrder(ana, luis);
		assertThat(byService.get(tinte)).containsExactly(ana);
	}

}
