package com.micita.micita_project.core.employee.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import com.micita.micita_project.core.employee.application.dto.request.UpdateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.mappers.EmployeeMapper;
import com.micita.micita_project.core.employee.application.ports.in.UpdateEmployeeUseCase;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeServicesCommandPort;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.exceptions.DuplicateEmployeeException;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-18. Appointments already in Firestore keep their own snapshot of the employee, so editing doesn't
 * touch them (ADR-001).
 */
@Service
@RequiredArgsConstructor
public class UpdateEmployeeCommandHandler implements UpdateEmployeeUseCase {

	private final BusinessQueryPort businessQuery;

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	private final EmployeeCommandRepositoryPort employeeCommandRepository;

	private final EmployeeServicesCommandPort employeeServices;

	private final EmployeeServicesQueryPort employeeServicesQuery;

	private final EmployeeMapper employeeMapper;

	private final Clock clock;

	/**
	 * Employee and service assignment share one transaction: rejected services roll the edit back.
	 */
	@Override
	@Transactional
	public EmployeeResponse update(UUID ownerId, UUID employeeId, UpdateEmployeeRequest request) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		Employee current = employeeQueryRepository.findOfBusiness(businessId, employeeId)
			.orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
		String email = EmployeeInput.email(request.email());
		String phone = EmployeeInput.optional(request.phone());
		// Same application-level check as HU-17 (D-23), ignoring the employee being edited.
		if (email != null && employeeQueryRepository.existsByEmailExcept(businessId, email, employeeId)) {
			throw DuplicateEmployeeException.email();
		}
		if (phone != null && employeeQueryRepository.existsByPhoneExcept(businessId, phone, employeeId)) {
			throw DuplicateEmployeeException.phone();
		}
		Employee employee = employeeCommandRepository.save(current.withDetails(request.firstName().trim(),
				request.lastName().trim(), phone, email, EmployeeStatus.valueOf(request.status()), Instant.now(clock)));
		Set<UUID> serviceIds = request.serviceIds() == null ? currentServiceIdsOf(businessId, employeeId)
				: employeeServices.replaceServices(businessId, employeeId, request.serviceIds());
		return employeeMapper.toResponse(employee, serviceIds);
	}

	private Set<UUID> currentServiceIdsOf(UUID businessId, UUID employeeId) {
		List<EmployeeServiceRef> services = employeeServicesQuery.findActiveServicesOf(businessId, List.of(employeeId))
			.getOrDefault(employeeId, List.of());
		Set<UUID> ids = new LinkedHashSet<>();
		services.forEach(service -> ids.add(service.id()));
		return ids;
	}

}
