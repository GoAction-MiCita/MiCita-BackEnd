package com.micita.micita_project.core.employee.infrastructure.adapters.out.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.UUID;

import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeBusinessQueryAdapterTest {

	@Mock
	private BusinessOwnershipQueryUseCase businessOwnershipQuery;

	@InjectMocks
	private EmployeeBusinessQueryAdapter adapter;

	@Test
	void businessIdComesFromTheBusinessModule() {
		UUID owner = UUID.randomUUID();
		UUID business = UUID.randomUUID();
		given(businessOwnershipQuery.requireBusinessIdOf(owner)).willReturn(business);

		assertThat(adapter.requireBusinessIdOf(owner)).isEqualTo(business);
	}

}
