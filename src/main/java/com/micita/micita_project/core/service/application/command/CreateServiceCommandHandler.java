package com.micita.micita_project.core.service.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.dto.request.CreateServiceRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceResponse;
import com.micita.micita_project.core.service.application.mappers.ServiceMapper;
import com.micita.micita_project.core.service.application.ports.in.AssignServiceEmployeesUseCase;
import com.micita.micita_project.core.service.application.ports.in.CreateServiceUseCase;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.DuplicateServiceException;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CreateServiceCommandHandler implements CreateServiceUseCase {

	private final BusinessQueryPort businessQuery;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final ServiceCommandRepositoryPort serviceCommandRepository;

	private final AssignServiceEmployeesUseCase assignServiceEmployees;

	private final ServiceMapper serviceMapper;

	private final Clock clock;

	/**
	 * Service and employee assignment share one transaction: rejected employees roll the service back.
	 */
	@Override
	@Transactional
	public ServiceResponse create(UUID ownerId, CreateServiceRequest request) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		String name = request.name().trim();
		// No partial unique index without migrations (D-23), so the check is in the application.
		if (serviceQueryRepository.existsByName(businessId, name)) {
			throw new DuplicateServiceException();
		}
		Service service = serviceCommandRepository.save(Service.register(businessId, name,
				optional(request.description()), request.durationMinutes(), request.price(),
				ServiceStatus.valueOf(request.status()), Instant.now(clock)));
		Set<UUID> employeeIds = request.employeeIds() == null || request.employeeIds().isEmpty() ? Set.of()
				: assignServiceEmployees.replaceEmployeesOfService(businessId, service.id(), request.employeeIds());
		return serviceMapper.toResponse(service, employeeIds);
	}

	/** Optional fields sent blank are stored as absent. */
	private static String optional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
