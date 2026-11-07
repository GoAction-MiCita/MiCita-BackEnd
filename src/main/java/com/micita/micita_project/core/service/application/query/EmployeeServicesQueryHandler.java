package com.micita.micita_project.core.service.application.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.in.EmployeeServicesQueryUseCase;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceEmployeeQueryRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.InvalidServiceIdsException;
import com.micita.micita_project.core.service.domain.models.ServiceAssignment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeServicesQueryHandler implements EmployeeServicesQueryUseCase {

	private final ServiceEmployeeQueryRepositoryPort serviceEmployeeQueryRepository;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

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

	@Override
	public Set<UUID> findEmployeeIdsProviding(UUID businessId, Collection<UUID> serviceIds) {
		Set<UUID> requested = new LinkedHashSet<>(serviceIds);
		if (requested.isEmpty()) {
			return Set.of();
		}
		// Deleted services are hidden from the admin, so filtering by one is the same as by an unknown id.
		Set<UUID> available = serviceQueryRepository.findAssignableIds(businessId, requested);
		List<UUID> rejected = requested.stream().filter(id -> !available.contains(id)).toList();
		if (!rejected.isEmpty()) {
			throw new InvalidServiceIdsException(rejected);
		}
		return serviceEmployeeQueryRepository.findEmployeeIdsAssignedTo(requested);
	}

}
