package com.micita.micita_project.core.employee.infrastructure.adapters.out.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
import com.micita.micita_project.core.service.application.ports.in.EmployeeServicesQueryUseCase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reads {@code service_employees} through the port the service module exposes (ADR-002).
 */
@Component
@RequiredArgsConstructor
public class EmployeeServicesQueryModuleAdapter implements EmployeeServicesQueryPort {

	private final EmployeeServicesQueryUseCase employeeServicesQuery;

	@Override
	public Map<UUID, List<EmployeeServiceRef>> findActiveServicesOf(UUID businessId, Collection<UUID> employeeIds) {
		return employeeServicesQuery.findActiveServicesOf(businessId, employeeIds)
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey,
					entry -> entry.getValue()
						.stream()
						.map(service -> new EmployeeServiceRef(service.id(), service.name()))
						.toList()));
	}

	@Override
	public Set<UUID> findEmployeeIdsProviding(UUID businessId, Collection<UUID> serviceIds) {
		return employeeServicesQuery.findEmployeeIdsProviding(businessId, serviceIds);
	}

}
