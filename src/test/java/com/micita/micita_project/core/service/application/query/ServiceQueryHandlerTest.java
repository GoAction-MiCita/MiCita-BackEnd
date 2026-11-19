package com.micita.micita_project.core.service.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HU-11: the bookable-services port only returns what the repository reports as {@code ACTIVE}.
 */
@ExtendWith(MockitoExtension.class)
class ServiceQueryHandlerTest {

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	private static final UUID ACTIVE = UUID.fromString("5c2d8e4f-1a3b-4c5d-9e6f-7a8b9c0d1e2f");

	private static final UUID INACTIVE = UUID.fromString("6d3e9f5a-2b4c-4d6e-8f7a-8b9c0d1e2f3a");

	@Mock
	private ServiceQueryRepositoryPort serviceQueryRepository;

	@InjectMocks
	private ServiceQueryHandler handler;

	@Test
	void returnsOnlyTheBookableIds() {
		given(serviceQueryRepository.findBookableIds(BUSINESS_ID, Set.of(ACTIVE, INACTIVE))).willReturn(Set.of(ACTIVE));

		assertThat(handler.findBookableIds(BUSINESS_ID, List.of(ACTIVE, INACTIVE))).containsExactly(ACTIVE);
	}

	@Test
	void emptyInputDoesNotQuery() {
		assertThat(handler.findBookableIds(BUSINESS_ID, List.of())).isEmpty();
		verifyNoInteractions(serviceQueryRepository);
	}

}
