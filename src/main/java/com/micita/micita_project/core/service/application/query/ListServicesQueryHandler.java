package com.micita.micita_project.core.service.application.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidPageRequestException;
import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.application.dto.request.ServiceListFilter;
import com.micita.micita_project.core.service.application.dto.response.ServiceEmployeeResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceListItemResponse;
import com.micita.micita_project.core.service.application.ports.in.ListServicesUseCase;
import com.micita.micita_project.core.service.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.EmployeeQueryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceEmployeeQueryRepositoryPort;
import com.micita.micita_project.core.service.application.ports.out.query.ServiceQueryRepositoryPort;
import com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException;
import com.micita.micita_project.core.service.domain.models.Service;
import com.micita.micita_project.core.service.domain.models.ServiceCriteria;
import com.micita.micita_project.core.service.domain.models.ServiceEmployee;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ListServicesQueryHandler implements ListServicesUseCase {

	static final int MAX_PAGE_SIZE = 50;

	static final int MAX_NAME_LENGTH = 100;

	private static final Set<String> FILTERABLE_STATUSES = Set.of(ServiceStatus.ACTIVE.name(),
			ServiceStatus.INACTIVE.name());

	private static final Comparator<ServiceEmployee> BY_NAME = Comparator
		.comparing((ServiceEmployee employee) -> employee.firstName().toLowerCase())
		.thenComparing(employee -> employee.lastName().toLowerCase())
		.thenComparing(ServiceEmployee::id);

	private final BusinessQueryPort businessQuery;

	private final ServiceQueryRepositoryPort serviceQueryRepository;

	private final ServiceEmployeeQueryRepositoryPort serviceEmployeeQueryRepository;

	private final EmployeeQueryPort employeeQuery;

	@Override
	public PageResponse<ServiceListItemResponse> list(UUID ownerId, ServiceListFilter filter, int page, int size) {
		String name = blankToNull(filter.name());
		String status = blankToNull(filter.status());
		validate(page, size, name, status);
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);

		Set<UUID> matchingServiceIds = null;
		if (filter.employeeIds() != null && !filter.employeeIds().isEmpty()) {
			requireAssignable(businessId, filter.employeeIds());
			matchingServiceIds = serviceEmployeeQueryRepository.findServiceIdsAssignedTo(filter.employeeIds());
			if (matchingServiceIds.isEmpty()) {
				// None of those employees provides a service: no need to query the services.
				return new PageResponse<>(List.of(), page, size, 0, 0);
			}
		}
		ServiceCriteria criteria = new ServiceCriteria(name, status == null ? null : ServiceStatus.valueOf(status),
				matchingServiceIds);

		PageResponse<Service> services = serviceQueryRepository.findPageOfBusiness(businessId, criteria, page, size);
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

	private void requireAssignable(UUID businessId, List<UUID> employeeIds) {
		Set<UUID> valid = employeeQuery.findAssignableIds(businessId, employeeIds);
		List<UUID> rejected = new ArrayList<>(new LinkedHashSet<>(employeeIds));
		rejected.removeAll(valid);
		if (!rejected.isEmpty()) {
			throw new InvalidEmployeeIdsException(rejected);
		}
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static void validate(int page, int size, String name, String status) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (page < 0) {
			errors.put("page", "must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			errors.put("size", "must be between 1 and " + MAX_PAGE_SIZE);
		}
		if (name != null && name.length() > MAX_NAME_LENGTH) {
			errors.put("name", "must be at most " + MAX_NAME_LENGTH + " characters");
		}
		// Case-sensitive on purpose: only the uppercase values are accepted.
		if (status != null && !FILTERABLE_STATUSES.contains(status)) {
			errors.put("status", "must be ACTIVE or INACTIVE");
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
