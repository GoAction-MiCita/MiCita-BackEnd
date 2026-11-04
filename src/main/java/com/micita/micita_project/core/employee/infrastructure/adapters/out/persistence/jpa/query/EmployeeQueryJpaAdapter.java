package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.query;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities.EmployeeEntity;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.mappers.EmployeeBDMapper;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeQueryJpaAdapter implements EmployeeQueryRepositoryPort {

	private final EmployeeJpaRepository employeeJpaRepository;

	private final EmployeeBDMapper employeeBDMapper;

	@Override
	public boolean existsByEmail(UUID businessId, String email) {
		return employeeJpaRepository.existsByBusinessIdAndEmailAndStatusNot(businessId, email, EmployeeStatus.DELETED);
	}

	@Override
	public boolean existsByPhone(UUID businessId, String phone) {
		return employeeJpaRepository.existsByBusinessIdAndPhoneAndStatusNot(businessId, phone, EmployeeStatus.DELETED);
	}

	@Override
	public Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds) {
		return employeeJpaRepository.findIdsOfBusiness(businessId, employeeIds, EmployeeStatus.DELETED);
	}

	@Override
	public PageResponse<Employee> findPageOfBusiness(UUID businessId, int page, int size) {
		Page<EmployeeEntity> result = employeeJpaRepository.findByBusinessIdAndStatusNot(businessId,
				EmployeeStatus.DELETED,
				PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending())));
		List<Employee> content = result.getContent().stream().map(employeeBDMapper::toDomain).toList();
		return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(),
				result.getTotalPages());
	}

	@Override
	public Set<UUID> findBookableIds(UUID businessId, Collection<UUID> employeeIds) {
		return employeeJpaRepository.findIdsOfBusinessWithStatus(businessId, employeeIds, EmployeeStatus.ACTIVE);
	}

}
