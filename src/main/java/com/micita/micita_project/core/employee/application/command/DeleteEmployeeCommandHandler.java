package com.micita.micita_project.core.employee.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.employee.application.ports.in.DeleteEmployeeUseCase;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.FutureAppointmentsQueryPort;
import com.micita.micita_project.core.employee.domain.exceptions.EmployeeHasFutureAppointmentsException;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-21. Soft delete only: the row and its {@code service_employees} rows are kept, and every query
 * already leaves {@code DELETED} employees out. Appointments live in Firestore, so the future-appointment
 * check and the update are not one transaction (ADR-001); an appointment booked in between is possible.
 */
@Service
@RequiredArgsConstructor
public class DeleteEmployeeCommandHandler implements DeleteEmployeeUseCase {

	private final BusinessQueryPort businessQuery;

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	private final EmployeeCommandRepositoryPort employeeCommandRepository;

	private final FutureAppointmentsQueryPort futureAppointmentsQuery;

	private final Clock clock;

	@Override
	@Transactional
	public void delete(UUID ownerId, UUID employeeId) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		Employee employee = employeeQueryRepository.findOfBusiness(businessId, employeeId)
			.orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
		Instant now = Instant.now(clock);
		long futureAppointments = futureAppointmentsQuery.countFutureAppointmentsOf(businessId, employeeId, now);
		if (futureAppointments > 0) {
			throw new EmployeeHasFutureAppointmentsException(futureAppointments);
		}
		employeeCommandRepository.save(employee.withStatus(EmployeeStatus.DELETED, now));
	}

}
