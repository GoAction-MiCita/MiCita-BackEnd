package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities.EmployeeEntity;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.mappers.EmployeeBDMapper;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeCommandJpaAdapterTest {

	private static final UUID ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private EmployeeJpaRepository employeeJpaRepository;

	@Spy
	private EmployeeBDMapper employeeBDMapper = Mappers.getMapper(EmployeeBDMapper.class);

	@InjectMocks
	private EmployeeCommandJpaAdapter adapter;

	@Test
	void saveMapsBothWaysAndReturnsGeneratedId() {
		given(employeeJpaRepository.save(any())).willAnswer(invocation -> {
			EmployeeEntity entity = invocation.getArgument(0);
			entity.setId(ID);
			return entity;
		});

		Employee saved = adapter.save(Employee.register(BUSINESS_ID, "Carlos", "Gómez", "+573001234567",
				"carlos@elcorte.com", EmployeeStatus.ACTIVE, NOW));

		ArgumentCaptor<EmployeeEntity> entity = ArgumentCaptor.forClass(EmployeeEntity.class);
		verify(employeeJpaRepository).save(entity.capture());
		assertThat(entity.getValue().getBusinessId()).isEqualTo(BUSINESS_ID);
		assertThat(entity.getValue().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
		assertThat(saved).isEqualTo(new Employee(ID, BUSINESS_ID, "Carlos", "Gómez", "+573001234567",
				"carlos@elcorte.com", EmployeeStatus.ACTIVE, NOW, NOW));
	}

}
