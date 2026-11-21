package com.micita.micita_project.core.service.application.query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.service.application.dto.response.ServiceDetailResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceEmployeeResponse;
import com.micita.micita_project.core.service.application.ports.in.GetServiceUseCase;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.EmployeeQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceEmployeeQueryRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.Service;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class GetServiceQueryHandler implements GetServiceUseCase {

	private final BusinessQueryPort businessQuery;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final ServiceEmployeeQueryRepositoryPort serviceEmployeeQueryRepository;

	private final EmployeeQueryPort employeeQuery;

	@Override
	public ServiceDetailResponse get(UUID ownerId, UUID serviceId) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		Service service = serviceQueryRepository.findOfBusiness(businessId, serviceId)
			.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		Set<UUID> assignedIds = serviceEmployeeQueryRepository.findEmployeeIdsByService(List.of(serviceId))
			.getOrDefault(serviceId, Set.of());
		// Deleted, unknown and foreign employees don't come back from the employee module.
		List<ServiceEmployeeResponse> employees = assignedIds.isEmpty() ? List.of()
				: employeeQuery.findEmployees(businessId, assignedIds)
					.stream()
					.sorted(ListServicesQueryHandler.BY_NAME)
					// Employees have no photo yet (E03), so it is always null.
					.map(employee -> new ServiceEmployeeResponse(employee.id(), employee.firstName(),
							employee.lastName(), null))
					.toList();
		return new ServiceDetailResponse(service.id(), service.name(), service.description(),
				service.durationMinutes(), service.price(), service.status().name(), employees, service.createdAt(),
				service.updatedAt());
	}

}
