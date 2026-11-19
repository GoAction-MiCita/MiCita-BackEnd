package com.micita.micita_project.core.service.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.service.application.dto.request.ChangeServiceStatusRequest;
import com.micita.micita_project.core.service.application.dto.response.ServiceStatusResponse;
import com.micita.micita_project.core.service.application.ports.in.ChangeServiceStatusUseCase;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-11. Only the service row changes: employee assignments stay in {@code service_employees} and
 * appointments in Firestore are not touched (ADR-001). Bookability follows from the status, since
 * {@code BookableServicesQueryPort} only returns {@code ACTIVE} services.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ChangeServiceStatusCommandHandler implements ChangeServiceStatusUseCase {

	private final BusinessQueryPort businessQuery;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final ServiceCommandRepositoryPort serviceCommandRepository;

	private final Clock clock;

	@Override
	@Transactional
	public ServiceStatusResponse changeStatus(UUID ownerId, UUID serviceId, ChangeServiceStatusRequest request) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		Service service = serviceQueryRepository.findOfBusiness(businessId, serviceId)
			.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		ServiceStatus status = ServiceStatus.valueOf(request.status());
		if (service.status() != status) {
			service = serviceCommandRepository.save(service.withStatus(status, Instant.now(clock)));
		}
		return new ServiceStatusResponse(service.id(), service.status().name());
	}

}
