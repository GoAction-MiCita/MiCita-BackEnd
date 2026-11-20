package com.micita.micita_project.core.service.application.command;

import java.util.List;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.service.application.dto.request.ReplaceServiceEmployeesRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceEmployeesResponse;
import com.micita.micita_project.core.service.application.ports.in.AssignServiceEmployeesUseCase;
import com.micita.micita_project.core.service.application.ports.in.ReplaceServiceEmployeesUseCase;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-12. Only {@code service_employees} changes; the service row (status, {@code updatedAt}) is left as is.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ReplaceServiceEmployeesCommandHandler implements ReplaceServiceEmployeesUseCase {

	private final BusinessQueryPort businessQuery;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final AssignServiceEmployeesUseCase assignServiceEmployees;

	@Override
	@Transactional
	public ServiceEmployeesResponse replaceEmployees(UUID ownerId, UUID serviceId,
			ReplaceServiceEmployeesRequest request) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		serviceQueryRepository.findOfBusiness(businessId, serviceId)
			.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		List<UUID> assigned = List
			.copyOf(assignServiceEmployees.replaceEmployeesOfService(businessId, serviceId, request.employeeIds()));
		return new ServiceEmployeesResponse(serviceId, assigned);
	}

}
