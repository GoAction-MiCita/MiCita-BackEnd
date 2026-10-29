package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEmployeeEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceEmployeeJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceEmployeeCommandJpaAdapterTest {

	@Mock
	private ServiceEmployeeJpaRepository serviceEmployeeJpaRepository;

	@InjectMocks
	private ServiceEmployeeCommandJpaAdapter adapter;

	@Test
	@SuppressWarnings("unchecked")
	void replacingDeletesTheOldRowsBeforeSavingTheNewOnes() {
		UUID employeeId = UUID.randomUUID();
		UUID serviceId = UUID.randomUUID();

		adapter.replaceServicesOfEmployee(employeeId, Set.of(serviceId));

		InOrder order = inOrder(serviceEmployeeJpaRepository);
		order.verify(serviceEmployeeJpaRepository).deleteByEmployeeId(employeeId);
		ArgumentCaptor<List<ServiceEmployeeEntity>> rows = ArgumentCaptor.forClass(List.class);
		order.verify(serviceEmployeeJpaRepository).saveAll(rows.capture());
		assertThat(rows.getValue()).singleElement().satisfies(row -> {
			assertThat(row.getServiceId()).isEqualTo(serviceId);
			assertThat(row.getEmployeeId()).isEqualTo(employeeId);
		});
	}

	@Test
	void compositeKeysWithTheSameIdsAreEqual() {
		UUID serviceId = UUID.randomUUID();
		UUID employeeId = UUID.randomUUID();

		assertThat(new ServiceEmployeeEntity.Key(serviceId, employeeId))
			.isEqualTo(new ServiceEmployeeEntity.Key(serviceId, employeeId))
			.hasSameHashCodeAs(new ServiceEmployeeEntity.Key(serviceId, employeeId))
			.isNotEqualTo(new ServiceEmployeeEntity.Key(serviceId, UUID.randomUUID()));
	}

}
