package com.micita.micita_project.core.service.application.command;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.in.AssignEmployeeServicesUseCase;
import com.micita.micita_project.core.service.application.ports.out.command.ServiceEmployeeCommandRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.InvalidServiceIdsException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignEmployeeServicesCommandHandler implements AssignEmployeeServicesUseCase {

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final ServiceEmployeeCommandRepositoryPort serviceEmployeeCommandRepository;

	public AssignEmployeeServicesCommandHandler(ServiceQueryRepositoryPort serviceQueryRepository,
			ServiceEmployeeCommandRepositoryPort serviceEmployeeCommandRepository) {
		this.serviceQueryRepository = serviceQueryRepository;
		this.serviceEmployeeCommandRepository = serviceEmployeeCommandRepository;
	}

	@Override
	@Transactional
	public Set<UUID> replaceServicesOfEmployee(UUID businessId, UUID employeeId, Collection<UUID> serviceIds) {
		Set<UUID> requested = new LinkedHashSet<>(serviceIds);
		if (!requested.isEmpty()) {
			// ACTIVE and INACTIVE services can be assigned; DELETED ones and other businesses' cannot.
			Set<UUID> assignable = serviceQueryRepository.findAssignableIds(businessId, requested);
			List<UUID> rejected = requested.stream().filter(id -> !assignable.contains(id)).toList();
			if (!rejected.isEmpty()) {
				throw new InvalidServiceIdsException(rejected);
			}
		}
		serviceEmployeeCommandRepository.replaceServicesOfEmployee(employeeId, requested);
		return requested;
	}

}
