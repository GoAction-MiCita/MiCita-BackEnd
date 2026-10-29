package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Deleted employees never count: not for duplicates, not as assignable.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeQueryJpaAdapterTest {

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	@Mock
	private EmployeeJpaRepository employeeJpaRepository;

	@InjectMocks
	private EmployeeQueryJpaAdapter adapter;

	@Test
	void duplicateLookupsIgnoreDeletedEmployees() {
		given(employeeJpaRepository.existsByBusinessIdAndEmailAndStatusNot(BUSINESS_ID, "carlos@elcorte.com",
				EmployeeStatus.DELETED))
			.willReturn(true);
		given(employeeJpaRepository.existsByBusinessIdAndPhoneAndStatusNot(BUSINESS_ID, "+573001234567",
				EmployeeStatus.DELETED))
			.willReturn(true);

		assertThat(adapter.existsByEmail(BUSINESS_ID, "carlos@elcorte.com")).isTrue();
		assertThat(adapter.existsByPhone(BUSINESS_ID, "+573001234567")).isTrue();
		assertThat(adapter.existsByEmail(BUSINESS_ID, "other@elcorte.com")).isFalse();
	}

	@Test
	void assignableIdsExcludeDeletedEmployees() {
		UUID employeeId = UUID.randomUUID();
		given(employeeJpaRepository.findIdsOfBusiness(BUSINESS_ID, List.of(employeeId), EmployeeStatus.DELETED))
			.willReturn(Set.of(employeeId));

		assertThat(adapter.findAssignableIds(BUSINESS_ID, List.of(employeeId))).containsExactly(employeeId);
	}

}
