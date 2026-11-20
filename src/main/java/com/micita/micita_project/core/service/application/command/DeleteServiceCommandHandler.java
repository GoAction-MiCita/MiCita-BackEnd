package com.micita.micita_project.core.service.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.service.application.ports.in.DeleteServiceUseCase;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.FutureAppointmentsQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.ServiceHasFutureAppointmentsException;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-14. Soft delete only: the row and its {@code service_employees} rows are kept, and every query
 * already leaves {@code DELETED} services out. Appointments live in Firestore, so the future-appointment
 * check and the update are not one transaction (ADR-001); an appointment booked in between is possible.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class DeleteServiceCommandHandler implements DeleteServiceUseCase {

	private final BusinessQueryPort businessQuery;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final ServiceCommandRepositoryPort serviceCommandRepository;

	private final FutureAppointmentsQueryPort futureAppointmentsQuery;

	private final Clock clock;

	@Override
	@Transactional
	public void delete(UUID ownerId, UUID serviceId) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		Service service = serviceQueryRepository.findOfBusiness(businessId, serviceId)
			.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		Instant now = Instant.now(clock);
		long futureAppointments = futureAppointmentsQuery.countFutureAppointmentsOf(businessId, serviceId, now);
		if (futureAppointments > 0) {
			throw new ServiceHasFutureAppointmentsException(futureAppointments);
		}
		serviceCommandRepository.save(service.withStatus(ServiceStatus.DELETED, now));
	}

}
