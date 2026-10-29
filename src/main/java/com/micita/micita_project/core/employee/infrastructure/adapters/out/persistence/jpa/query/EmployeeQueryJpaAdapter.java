package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories.EmployeeJpaRepository;

import org.springframework.stereotype.Component;

@Component
public class EmployeeQueryJpaAdapter implements EmployeeQueryRepositoryPort {

	private final EmployeeJpaRepository employeeJpaRepository;

	public EmployeeQueryJpaAdapter(EmployeeJpaRepository employeeJpaRepository) {
		this.employeeJpaRepository = employeeJpaRepository;
	}

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

}
