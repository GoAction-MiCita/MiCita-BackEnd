package com.micita.micita_project.core.auth.infrastructure.adapters.out.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.UUID;

import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-04: the {@code hasBusiness} reported at login reflects what the business module stores.
 */
@ExtendWith(MockitoExtension.class)
class BusinessModuleQueryAdapterTest {

	@Mock
	private BusinessOwnershipQueryUseCase businessOwnershipQuery;

	@InjectMocks
	private BusinessModuleQueryAdapter adapter;

	@Test
	void hasBusinessComesFromTheBusinessModule() {
		UUID owner = UUID.randomUUID();
		given(businessOwnershipQuery.ownsBusiness(owner)).willReturn(true);

		assertThat(adapter.existsForOwner(owner)).isTrue();
		assertThat(adapter.existsForOwner(UUID.randomUUID())).isFalse();
	}

}
