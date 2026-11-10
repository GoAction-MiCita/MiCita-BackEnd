package com.micita.micita_project.core.service.infrastructure.adapters.out.employee;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.ports.in.EmployeeQueryUseCase;
import com.micita.micita_project.core.service.application.ports.out.query.EmployeeQueryPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates employees through the port the employee module exposes (ADR-002).
 */
@Component
@RequiredArgsConstructor
public class ServiceEmployeeQueryModuleAdapter implements EmployeeQueryPort {

	private final EmployeeQueryUseCase employeeQuery;

	@Override
	public Set<UUID> findAssignableIds(UUID businessId, Collection<UUID> employeeIds) {
		return employeeQuery.findAssignableIds(businessId, employeeIds);
	}

}
