package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.UUID;

import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories.BusinessJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessQueryJpaAdapterTest {

	@Mock
	private BusinessJpaRepository businessJpaRepository;

	@InjectMocks
	private BusinessQueryJpaAdapter adapter;

	@Test
	void existsByOwnerIdDelegatesToRepository() {
		UUID ownerId = UUID.randomUUID();
		given(businessJpaRepository.existsByOwnerId(ownerId)).willReturn(true);

		assertThat(adapter.existsByOwnerId(ownerId)).isTrue();
		assertThat(adapter.existsByOwnerId(UUID.randomUUID())).isFalse();
	}

}
