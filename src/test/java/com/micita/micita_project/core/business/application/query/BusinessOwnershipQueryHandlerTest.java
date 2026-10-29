package com.micita.micita_project.core.business.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.core.business.application.ports.out.query.BusinessQueryRepositoryPort;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessOwnershipQueryHandlerTest {

	@Mock
	private BusinessQueryRepositoryPort businessQueryRepository;

	@InjectMocks
	private BusinessOwnershipQueryHandler handler;

	@Test
	void ownershipComesFromTheRepository() {
		UUID owner = UUID.randomUUID();
		UUID other = UUID.randomUUID();
		given(businessQueryRepository.existsByOwnerId(owner)).willReturn(true);
		given(businessQueryRepository.existsByOwnerId(other)).willReturn(false);

		assertThat(handler.ownsBusiness(owner)).isTrue();
		assertThat(handler.ownsBusiness(other)).isFalse();
	}

	@Test
	void businessIdOfTheOwnerComesFromTheRepository() {
		UUID owner = UUID.randomUUID();
		UUID business = UUID.randomUUID();
		given(businessQueryRepository.findIdByOwnerId(owner)).willReturn(Optional.of(business));

		assertThat(handler.requireBusinessIdOf(owner)).isEqualTo(business);
	}

	@Test
	void userWithoutBusinessIsForbidden() {
		UUID owner = UUID.randomUUID();
		given(businessQueryRepository.findIdByOwnerId(owner)).willReturn(Optional.empty());

		assertThatThrownBy(() -> handler.requireBusinessIdOf(owner)).isInstanceOf(BusinessRequiredException.class);
	}

}
