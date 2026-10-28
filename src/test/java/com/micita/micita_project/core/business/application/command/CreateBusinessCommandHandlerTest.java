package com.micita.micita_project.core.business.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import com.micita.micita_project.core.business.application.dto.request.CreateBusinessRequest;
import com.micita.micita_project.core.business.application.dto.response.BusinessResponse;
import com.micita.micita_project.core.business.application.mappers.BusinessMapper;
import com.micita.micita_project.core.business.application.ports.out.command.BusinessCommandRepositoryPort;
import com.micita.micita_project.core.business.application.ports.out.query.BusinessQueryRepositoryPort;
import com.micita.micita_project.core.business.domain.exceptions.BusinessAlreadyExistsException;
import com.micita.micita_project.core.business.domain.models.Business;
import com.micita.micita_project.core.business.domain.models.BusinessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-03: a user without a business registers one, linked to them, active and in America/Bogota; a
 * user who already has one gets a conflict.
 */
@ExtendWith(MockitoExtension.class)
class CreateBusinessCommandHandlerTest {

	private static final UUID OWNER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final Instant NOW = Instant.parse("2026-10-27T15:00:00Z");

	@Mock
	private BusinessQueryRepositoryPort businessQueryRepository;

	@Mock
	private BusinessCommandRepositoryPort businessCommandRepository;

	private CreateBusinessCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new CreateBusinessCommandHandler(businessQueryRepository, businessCommandRepository,
				Mappers.getMapper(BusinessMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void createsActiveBusinessInBogotaLinkedToTheOwner() {
		given(businessQueryRepository.existsByOwnerId(OWNER_ID)).willReturn(false);
		given(businessCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));

		BusinessResponse response = handler.create(OWNER_ID, new CreateBusinessRequest(" El Corte ", " 900123456-7 ",
				" Calle 10 ", "+573001234567", " Contacto@ElCorte.com "));

		ArgumentCaptor<Business> saved = ArgumentCaptor.forClass(Business.class);
		verify(businessCommandRepository).save(saved.capture());
		assertThat(saved.getValue()).isEqualTo(new Business(null, OWNER_ID, "El Corte", "900123456-7", "Calle 10",
				"+573001234567", "contacto@elcorte.com", BusinessStatus.ACTIVE, ZoneId.of("America/Bogota"), NOW));
		assertThat(response).isEqualTo(new BusinessResponse(BUSINESS_ID, "El Corte", "900123456-7", "Calle 10",
				"+573001234567", "contacto@elcorte.com", "ACTIVE", "America/Bogota", NOW));
	}

	@Test
	void blankOptionalFieldsAreStoredAsAbsent() {
		given(businessCommandRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0)));

		BusinessResponse response = handler.create(OWNER_ID,
				new CreateBusinessRequest("El Corte", " ", "Calle 10", "+573001234567", null));

		assertThat(response.taxId()).isNull();
		assertThat(response.email()).isNull();
	}

	@Test
	void userWithABusinessCannotCreateAnother() {
		given(businessQueryRepository.existsByOwnerId(OWNER_ID)).willReturn(true);

		assertThatThrownBy(() -> handler.create(OWNER_ID,
				new CreateBusinessRequest("El Corte", null, "Calle 10", "+573001234567", null)))
			.isInstanceOf(BusinessAlreadyExistsException.class)
			.hasMessage("User already has a business");
		verify(businessCommandRepository, never()).save(any());
	}

	private static Business withId(Business business) {
		return new Business(BUSINESS_ID, business.ownerId(), business.name(), business.taxId(), business.address(),
				business.phone(), business.email(), business.status(), business.timeZone(), business.createdAt());
	}

}
