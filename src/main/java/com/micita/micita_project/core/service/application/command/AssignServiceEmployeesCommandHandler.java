package com.micita.micita_project.core.service.application.command;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.in.AssignServiceEmployeesUseCase;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceEmployeeCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.EmployeeQueryPort;
import com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignServiceEmployeesCommandHandler implements AssignServiceEmployeesUseCase {

	private final EmployeeQueryPort employeeQuery;

	private final ServiceEmployeeCommandRepositoryPort serviceEmployeeCommandRepository;

	@Override
	@Transactional
	public Set<UUID> replaceEmployeesOfService(UUID businessId, UUID serviceId, Collection<UUID> employeeIds) {
		Set<UUID> requested = new LinkedHashSet<>(employeeIds);
		if (!requested.isEmpty()) {
			// ACTIVE and INACTIVE employees can be assigned; DELETED ones and other businesses' cannot.
			Set<UUID> assignable = employeeQuery.findAssignableIds(businessId, requested);
			List<UUID> rejected = requested.stream().filter(id -> !assignable.contains(id)).toList();
			if (!rejected.isEmpty()) {
				throw new InvalidEmployeeIdsException(rejected);
			}
		}
		serviceEmployeeCommandRepository.replaceEmployeesOfService(serviceId, requested);
		return requested;
	}

}
