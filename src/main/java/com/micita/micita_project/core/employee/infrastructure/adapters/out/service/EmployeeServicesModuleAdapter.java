package com.micita.micita_project.core.employee.infrastructure.adapters.out.service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.ports.out.command.EmployeeServicesCommandPort;
import com.micita.micita_project.core.service.application.ports.in.AssignEmployeeServicesUseCase;

import org.springframework.stereotype.Component;

/**
 * Writes {@code service_employees} through the port the service module exposes (ADR-002).
 */
@Component
public class EmployeeServicesModuleAdapter implements EmployeeServicesCommandPort {

	private final AssignEmployeeServicesUseCase assignEmployeeServices;

	public EmployeeServicesModuleAdapter(AssignEmployeeServicesUseCase assignEmployeeServices) {
		this.assignEmployeeServices = assignEmployeeServices;
	}

	@Override
	public Set<UUID> replaceServices(UUID businessId, UUID employeeId, Collection<UUID> serviceIds) {
		return assignEmployeeServices.replaceServicesOfEmployee(businessId, employeeId, serviceIds);
	}

}
