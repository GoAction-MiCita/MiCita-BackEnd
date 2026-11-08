package com.micita.micita_project.core.employee.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.employee.application.dto.request.ChangeEmployeeStatusRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.mappers.EmployeeMapper;
import com.micita.micita_project.core.employee.application.ports.in.ChangeEmployeeStatusUseCase;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-20. Only the employee row changes: service assignments stay in {@code service_employees} and
 * appointments in Firestore are not touched (ADR-001). Bookability follows from the status, since
 * {@code BookableEmployeesQueryPort} only returns {@code ACTIVE} employees.
 */
@Service
@RequiredArgsConstructor
public class ChangeEmployeeStatusCommandHandler implements ChangeEmployeeStatusUseCase {

	private final BusinessQueryPort businessQuery;

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	private final EmployeeCommandRepositoryPort employeeCommandRepository;

	private final EmployeeServicesQueryPort employeeServicesQuery;

	private final EmployeeMapper employeeMapper;

	private final Clock clock;

	@Override
	@Transactional
	public EmployeeResponse changeStatus(UUID ownerId, UUID employeeId, ChangeEmployeeStatusRequest request) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		Employee employee = employeeQueryRepository.findOfBusiness(businessId, employeeId)
			.orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
		EmployeeStatus status = EmployeeStatus.valueOf(request.status());
		if (employee.status() != status) {
			employee = employeeCommandRepository.save(employee.withStatus(status, Instant.now(clock)));
		}
		return employeeMapper.toResponse(employee, serviceIdsOf(businessId, employeeId));
	}

	private Set<UUID> serviceIdsOf(UUID businessId, UUID employeeId) {
		List<EmployeeServiceRef> services = employeeServicesQuery.findActiveServicesOf(businessId, List.of(employeeId))
			.getOrDefault(employeeId, List.of());
		Set<UUID> ids = new LinkedHashSet<>();
		services.forEach(service -> ids.add(service.id()));
		return ids;
	}

}
