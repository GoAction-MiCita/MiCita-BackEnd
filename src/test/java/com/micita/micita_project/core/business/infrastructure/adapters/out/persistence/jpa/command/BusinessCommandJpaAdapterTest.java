package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import com.micita.micita_project.core.business.domain.exceptions.BusinessAlreadyExistsException;
import com.micita.micita_project.core.business.domain.models.Business;
import com.micita.micita_project.core.business.domain.models.BusinessStatus;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.entities.BusinessEntity;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.mappers.BusinessBDMapper;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories.BusinessJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BusinessCommandJpaAdapterTest {

	private static final UUID ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final Instant NOW = Instant.parse("2026-10-27T15:00:00Z");

	@Mock
	private BusinessJpaRepository businessJpaRepository;

	@Spy
	private BusinessBDMapper businessBDMapper = Mappers.getMapper(BusinessBDMapper.class);

	@InjectMocks
	private BusinessCommandJpaAdapter adapter;

	@Test
	void saveMapsBothWaysAndReturnsGeneratedId() {
		given(businessJpaRepository.saveAndFlush(any())).willAnswer(invocation -> {
			BusinessEntity entity = invocation.getArgument(0);
			entity.setId(ID);
			return entity;
		});

		Business saved = adapter.save(business());

		ArgumentCaptor<BusinessEntity> entity = ArgumentCaptor.forClass(BusinessEntity.class);
		org.mockito.Mockito.verify(businessJpaRepository).saveAndFlush(entity.capture());
		assertThat(entity.getValue().getTimeZone()).isEqualTo("America/Bogota");
		assertThat(entity.getValue().getStatus()).isEqualTo(BusinessStatus.ACTIVE);
		assertThat(saved).isEqualTo(new Business(ID, OWNER_ID, "El Corte", "900123456-7", "Calle 10", "+573001234567",
				"contacto@elcorte.com", BusinessStatus.ACTIVE, ZoneId.of("America/Bogota"), NOW));
	}

	@Test
	void saveTranslatesUniqueOwnerViolationToConflict() {
		given(businessJpaRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> adapter.save(business())).isInstanceOf(BusinessAlreadyExistsException.class);
	}

	private static Business business() {
		return Business.register(OWNER_ID, "El Corte", "900123456-7", "Calle 10", "+573001234567",
				"contacto@elcorte.com", NOW);
	}

}
