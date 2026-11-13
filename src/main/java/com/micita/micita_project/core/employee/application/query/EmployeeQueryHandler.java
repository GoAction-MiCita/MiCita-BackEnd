package com.micita.micita_project.core.employee.application.query;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.ports.in.BookableEmployeesQueryPort;
import com.micita.micita_project.core.employee.application.ports.in.EmployeeQueryUseCase;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.domain.models.EmployeeSummary;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeQueryHandler implements EmployeeQueryUseCase, BookableEmployeesQueryPort {

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	@Override
	public Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds) {
		if (employeeIds.isEmpty()) {
			return Set.of();
		}
		return employeeQueryRepository.findAssignableIds(businessId, Set.copyOf(employeeIds));
	}

	@Override
	public List<EmployeeSummary> findSummaries(UUID businessId, Collection<UUID> employeeIds) {
		if (employeeIds.isEmpty()) {
			return List.of();
		}
		return employeeQueryRepository.findAllOfBusiness(businessId, Set.copyOf(employeeIds))
			.stream()
			.map(employee -> new EmployeeSummary(employee.id(), employee.firstName(), employee.lastName()))
			.toList();
	}

	@Override
	public Set<UUID> findBookableIds(UUID businessId, Collection<UUID> employeeIds) {
		if (employeeIds.isEmpty()) {
			return Set.of();
		}
		return employeeQueryRepository.findBookableIds(businessId, Set.copyOf(employeeIds));
	}

}
