package com.micita.micita_project.core.service.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.service.application.dto.request.UpdateServiceRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;
import com.micita.micita_project.core.service.application.mappers.ServiceMapper;
import com.micita.micita_project.core.service.application.ports.in.AssignServiceEmployeesUseCase;
import com.micita.micita_project.core.service.application.ports.in.UpdateServiceUseCase;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.DuplicateServiceException;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-10. Service and employee assignment share one transaction: rejected employees roll the edit back.
 * Last write wins, there is no optimistic locking in the MVP.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class UpdateServiceCommandHandler implements UpdateServiceUseCase {

	private final BusinessQueryPort businessQuery;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final ServiceCommandRepositoryPort serviceCommandRepository;

	private final AssignServiceEmployeesUseCase assignServiceEmployees;

	private final ServiceMapper serviceMapper;

	private final Clock clock;

	@Override
	@Transactional
	public ServiceResponse update(UUID ownerId, UUID serviceId, UpdateServiceRequest request) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		Service current = serviceQueryRepository.findOfBusiness(businessId, serviceId)
			.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		String name = request.name().trim();
		// No partial unique index without migrations (D-23), so the check is in the application. The
		// service itself doesn't count, so it can keep its name.
		if (serviceQueryRepository.existsByNameExcept(businessId, name, serviceId)) {
			throw new DuplicateServiceException();
		}
		Service saved = serviceCommandRepository.save(current.withDetails(name, optional(request.description()),
				request.durationMinutes(), request.price(), ServiceStatus.valueOf(request.status()),
				Instant.now(clock)));
		Set<UUID> employeeIds = assignServiceEmployees.replaceEmployeesOfService(businessId, serviceId,
				request.employeeIds());
		return serviceMapper.toResponse(saved, employeeIds);
	}

	/** Optional fields sent blank are stored as absent. */
	private static String optional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
