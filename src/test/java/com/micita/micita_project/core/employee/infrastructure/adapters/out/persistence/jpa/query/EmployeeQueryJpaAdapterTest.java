package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeCriteria;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities.EmployeeEntity;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.mappers.EmployeeBDMapper;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Deleted employees never count: not for duplicates, not as assignable.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeQueryJpaAdapterTest {

	private static final UUID BUSINESS_ID = UUID.fromString("7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");

	@Mock
	private EmployeeJpaRepository employeeJpaRepository;

	@Mock
	private EmployeeBDMapper employeeBDMapper;

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
	void duplicateLookupsForAnEditIgnoreTheEmployeeItselfAndDeletedOnes() {
		UUID employeeId = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");
		given(employeeJpaRepository.existsByBusinessIdAndEmailAndStatusNotAndIdNot(BUSINESS_ID, "ana@elcorte.com",
				EmployeeStatus.DELETED, employeeId))
			.willReturn(true);
		given(employeeJpaRepository.existsByBusinessIdAndPhoneAndStatusNotAndIdNot(BUSINESS_ID, "+573109876543",
				EmployeeStatus.DELETED, employeeId))
			.willReturn(true);

		assertThat(adapter.existsByEmailExcept(BUSINESS_ID, "ana@elcorte.com", employeeId)).isTrue();
		assertThat(adapter.existsByPhoneExcept(BUSINESS_ID, "+573109876543", employeeId)).isTrue();
		assertThat(adapter.existsByEmailExcept(BUSINESS_ID, "carlos@elcorte.com", employeeId)).isFalse();
		assertThat(adapter.existsByPhoneExcept(BUSINESS_ID, "+573001234567", employeeId)).isFalse();
	}

	@Test
	void findOfBusinessIgnoresDeletedEmployeesAndOtherBusinesses() {
		UUID employeeId = UUID.randomUUID();
		EmployeeEntity entity = new EmployeeEntity();
		Employee employee = new Employee(employeeId, BUSINESS_ID, "Carlos", "Gómez", null, null, EmployeeStatus.ACTIVE,
				Instant.now(), Instant.now());
		given(employeeJpaRepository.findByIdAndBusinessIdAndStatusNot(employeeId, BUSINESS_ID, EmployeeStatus.DELETED))
			.willReturn(Optional.of(entity));
		given(employeeBDMapper.toDomain(entity)).willReturn(employee);

		assertThat(adapter.findOfBusiness(BUSINESS_ID, employeeId)).contains(employee);
		assertThat(adapter.findOfBusiness(UUID.randomUUID(), employeeId)).isEmpty();
	}

	@Test
	void assignableIdsExcludeDeletedEmployees() {
		UUID employeeId = UUID.randomUUID();
		given(employeeJpaRepository.findIdsOfBusiness(BUSINESS_ID, List.of(employeeId), EmployeeStatus.DELETED))
			.willReturn(Set.of(employeeId));

		assertThat(adapter.findAssignableIds(BUSINESS_ID, List.of(employeeId))).containsExactly(employeeId);
	}

	@Test
	void pageOfBusinessExcludesDeletedAndOrdersByCreationThenId() {
		EmployeeEntity entity = new EmployeeEntity();
		Employee employee = new Employee(UUID.randomUUID(), BUSINESS_ID, "Carlos", "Gómez", null, null,
				EmployeeStatus.ACTIVE, Instant.now(), Instant.now());
		Page<EmployeeEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(1, 5), 6);
		given(employeeJpaRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(page);
		given(employeeBDMapper.toDomain(entity)).willReturn(employee);

		PageResponse<Employee> result = adapter.findPageOfBusiness(BUSINESS_ID, EmployeeCriteria.none(), 1, 5);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(employeeJpaRepository).findAll(any(Specification.class), pageable.capture());
		assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
		assertThat(pageable.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
		assertThat(result.content()).containsExactly(employee);
		assertThat(result.page()).isEqualTo(1);
		assertThat(result.size()).isEqualTo(5);
		assertThat(result.totalElements()).isEqualTo(6);
		assertThat(result.totalPages()).isEqualTo(2);
	}

	@Test
	void likeWildcardsInTheNameAreEscaped() {
		assertThat(EmployeeQueryJpaAdapter.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
		assertThat(EmployeeQueryJpaAdapter.escapeLike("ana")).isEqualTo("ana");
	}

	@Test
	void bookableIdsAreTheActiveOnes() {
		UUID employeeId = UUID.randomUUID();
		given(employeeJpaRepository.findIdsOfBusinessWithStatus(BUSINESS_ID, List.of(employeeId),
				EmployeeStatus.ACTIVE)).willReturn(Set.of(employeeId));

		assertThat(adapter.findBookableIds(BUSINESS_ID, List.of(employeeId))).containsExactly(employeeId);
	}

}
