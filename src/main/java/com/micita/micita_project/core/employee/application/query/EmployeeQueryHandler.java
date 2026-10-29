package com.micita.micita_project.core.employee.application.query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.ports.in.EmployeeQueryUseCase;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;

import org.springframework.stereotype.Service;

@Service
public class EmployeeQueryHandler implements EmployeeQueryUseCase {

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	public EmployeeQueryHandler(EmployeeQueryRepositoryPort employeeQueryRepository) {
		this.employeeQueryRepository = employeeQueryRepository;
	}

	@Override
	public Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds) {
		if (employeeIds.isEmpty()) {
			return Set.of();
		}
		return employeeQueryRepository.findAssignableIds(businessId, Set.copyOf(employeeIds));
	}

}
