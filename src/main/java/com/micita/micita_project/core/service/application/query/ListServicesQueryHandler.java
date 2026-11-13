package com.micita.micita_project.core.service.application.query;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidPageRequestException;
import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceEmployeeResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceListItemResponse;
import com.micita.micita_project.core.service.application.ports.in.ListServicesUseCase;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.EmployeeQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceEmployeeQueryRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceEmployee;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ListServicesQueryHandler implements ListServicesUseCase {

	static final int MAX_PAGE_SIZE = 50;

	private static final Comparator<ServiceEmployee> BY_NAME = Comparator
		.comparing((ServiceEmployee employee) -> employee.firstName().toLowerCase())
		.thenComparing(employee -> employee.lastName().toLowerCase())
		.thenComparing(ServiceEmployee::id);

	private final BusinessQueryPort businessQuery;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final ServiceEmployeeQueryRepositoryPort serviceEmployeeQueryRepository;

	private final EmployeeQueryPort employeeQuery;

	@Override
	public PageResponse<ServiceListItemResponse> list(UUID ownerId, int page, int size) {
		validate(page, size);
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);

		PageResponse<Service> services = serviceQueryRepository.findPageOfBusiness(businessId, page, size);
		if (services.content().isEmpty()) {
			return new PageResponse<>(List.of(), services.page(), services.size(), services.totalElements(),
					services.totalPages());
		}
		// Two lookups for the whole page, so the listing never queries employees per service.
		List<UUID> serviceIds = services.content().stream().map(Service::id).toList();
		Map<UUID, Set<UUID>> employeeIdsByService = serviceEmployeeQueryRepository.findEmployeeIdsByService(serviceIds);
		Set<UUID> employeeIds = new LinkedHashSet<>();
		employeeIdsByService.values().forEach(employeeIds::addAll);
		Map<UUID, ServiceEmployee> employees = new LinkedHashMap<>();
		if (!employeeIds.isEmpty()) {
			// Deleted, unknown and foreign employees don't come back, so they are dropped below.
			employeeQuery.findEmployees(businessId, employeeIds)
				.forEach(employee -> employees.put(employee.id(), employee));
		}

		List<ServiceListItemResponse> content = services.content()
			.stream()
			.map(service -> toItem(service, employeeIdsByService.getOrDefault(service.id(), Set.of()), employees))
			.toList();
		return new PageResponse<>(content, services.page(), services.size(), services.totalElements(),
				services.totalPages());
	}

	private static void validate(int page, int size) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (page < 0) {
			errors.put("page", "must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			errors.put("size", "must be between 1 and " + MAX_PAGE_SIZE);
		}
		if (!errors.isEmpty()) {
			throw new InvalidPageRequestException(errors);
		}
	}

	private static ServiceListItemResponse toItem(Service service, Set<UUID> assignedIds,
			Map<UUID, ServiceEmployee> employees) {
		List<ServiceEmployeeResponse> assigned = assignedIds.stream()
			.map(employees::get)
			.filter(java.util.Objects::nonNull)
			.sorted(BY_NAME)
			// Employees have no photo yet (E03), so it is always null.
			.map(employee -> new ServiceEmployeeResponse(employee.id(), employee.firstName(), employee.lastName(), null))
			.toList();
		return new ServiceListItemResponse(service.id(), service.name(), service.description(),
				service.durationMinutes(), service.price(), service.status().name(), assigned);
	}

}
