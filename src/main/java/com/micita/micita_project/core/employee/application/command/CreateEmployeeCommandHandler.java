package com.micita.micita_project.core.employee.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.dto.request.CreateEmployeeRequest;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeResponse;
import com.micita.micita_project.core.employee.application.mappers.EmployeeMapper;
import com.micita.micita_project.core.employee.application.ports.in.CreateEmployeeUseCase;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeCommandRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeServicesCommandPort;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.domain.exceptions.DuplicateEmployeeException;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEmployeeCommandHandler implements CreateEmployeeUseCase {

	private final BusinessQueryPort businessQuery;

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	private final EmployeeCommandRepositoryPort employeeCommandRepository;

	private final EmployeeServicesCommandPort employeeServices;

	private final EmployeeMapper employeeMapper;

	private final Clock clock;

	public CreateEmployeeCommandHandler(BusinessQueryPort businessQuery,
			EmployeeQueryRepositoryPort employeeQueryRepository, EmployeeCommandRepositoryPort employeeCommandRepository,
			EmployeeServicesCommandPort employeeServices, EmployeeMapper employeeMapper, Clock clock) {
		this.businessQuery = businessQuery;
		this.employeeQueryRepository = employeeQueryRepository;
		this.employeeCommandRepository = employeeCommandRepository;
		this.employeeServices = employeeServices;
		this.employeeMapper = employeeMapper;
		this.clock = clock;
	}

	/**
	 * Employee and service assignment share one transaction: rejected services roll the employee back.
	 */
	@Override
	@Transactional
	public EmployeeResponse create(UUID ownerId, CreateEmployeeRequest request) {
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		String email = optional(request.email());
		if (email != null) {
			email = email.toLowerCase(Locale.ROOT);
		}
		String phone = optional(request.phone());
		// No partial unique index without migrations (D-23), so the check is in the application.
		if (email != null && employeeQueryRepository.existsByEmail(businessId, email)) {
			throw DuplicateEmployeeException.email();
		}
		if (phone != null && employeeQueryRepository.existsByPhone(businessId, phone)) {
			throw DuplicateEmployeeException.phone();
		}
		Employee employee = employeeCommandRepository.save(Employee.register(businessId, request.firstName().trim(),
				request.lastName().trim(), phone, email, EmployeeStatus.valueOf(request.status()), Instant.now(clock)));
		Set<UUID> serviceIds = request.serviceIds() == null || request.serviceIds().isEmpty() ? Set.of()
				: employeeServices.replaceServices(businessId, employee.id(), request.serviceIds());
		return employeeMapper.toResponse(employee, serviceIds);
	}

	/** Optional fields sent blank are stored as absent. */
	private static String optional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
