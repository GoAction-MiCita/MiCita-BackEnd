package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.mappers.ServiceBDMapper;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories.ServiceJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceCommandJpaAdapterTest {

	private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");

	@Mock
	private ServiceJpaRepository serviceJpaRepository;

	@Test
	void savesTheServiceAndReturnsItWithTheGeneratedId() {
		UUID businessId = UUID.randomUUID();
		UUID generatedId = UUID.randomUUID();
		given(serviceJpaRepository.save(any(ServiceEntity.class))).willAnswer(invocation -> {
			ServiceEntity entity = invocation.getArgument(0);
			entity.setId(generatedId);
			return entity;
		});
		ServiceCommandJpaAdapter adapter = new ServiceCommandJpaAdapter(serviceJpaRepository,
				Mappers.getMapper(ServiceBDMapper.class));

		Service saved = adapter.save(Service.register(businessId, "Corte", "Clásico", 45, new BigDecimal("35000.00"),
				ServiceStatus.ACTIVE, NOW));

		ArgumentCaptor<ServiceEntity> entity = ArgumentCaptor.forClass(ServiceEntity.class);
		Mockito.verify(serviceJpaRepository).save(entity.capture());
		assertThat(entity.getValue().getBusinessId()).isEqualTo(businessId);
		assertThat(entity.getValue().getName()).isEqualTo("Corte");
		assertThat(entity.getValue().getStatus()).isEqualTo(ServiceStatus.ACTIVE);
		assertThat(saved).isEqualTo(new Service(generatedId, businessId, "Corte", "Clásico", 45,
				new BigDecimal("35000.00"), ServiceStatus.ACTIVE, NOW, NOW));
	}

}
