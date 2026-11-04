package com.micita.micita_project.core.service.application.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.in.EmployeeServicesQueryUseCase;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceEmployeeQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.ServiceAssignment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeServicesQueryHandler implements EmployeeServicesQueryUseCase {

	private final ServiceEmployeeQueryRepositoryPort serviceEmployeeQueryRepository;

	@Override
	public Map<UUID, List<ServiceSummary>> findActiveServicesOf(UUID businessId, Collection<UUID> employeeIds) {
		if (employeeIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, List<ServiceSummary>> servicesByEmployee = new HashMap<>();
		for (ServiceAssignment assignment : serviceEmployeeQueryRepository.findActiveAssignments(businessId,
				employeeIds)) {
			servicesByEmployee.computeIfAbsent(assignment.employeeId(), id -> new ArrayList<>())
				.add(new ServiceSummary(assignment.serviceId(), assignment.serviceName()));
		}
		return servicesByEmployee;
	}

}
