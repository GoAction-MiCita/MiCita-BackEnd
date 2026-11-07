package com.micita.micita_project.core.employee.application.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidPageRequestException;
import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.application.dto.request.EmployeeListFilter;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeListItemResponse;
import com.micita.micita_project.core.employee.application.ports.in.ListEmployeesUseCase;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeCriteria;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;
import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListEmployeesQueryHandler implements ListEmployeesUseCase {

	static final int MAX_PAGE_SIZE = 50;

	static final int MAX_NAME_LENGTH = 100;

	private static final Set<String> FILTERABLE_STATUSES = Set.of(EmployeeStatus.ACTIVE.name(),
			EmployeeStatus.INACTIVE.name());

	private final BusinessQueryPort businessQuery;

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	private final EmployeeServicesQueryPort employeeServicesQuery;

	@Override
	public PageResponse<EmployeeListItemResponse> list(UUID ownerId, EmployeeListFilter filter, int page, int size) {
		String name = blankToNull(filter.name());
		String status = blankToNull(filter.status());
		validate(page, size, name, status);
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);

		Set<UUID> employeeIds = null;
		if (filter.serviceIds() != null && !filter.serviceIds().isEmpty()) {
			employeeIds = employeeServicesQuery.findEmployeeIdsProviding(businessId, filter.serviceIds());
			if (employeeIds.isEmpty()) {
				// Nobody provides those services: no need to query the employees.
				return new PageResponse<>(List.of(), page, size, 0, 0);
			}
		}
		EmployeeCriteria criteria = new EmployeeCriteria(name, status == null ? null : EmployeeStatus.valueOf(status),
				employeeIds);

		PageResponse<Employee> employees = employeeQueryRepository.findPageOfBusiness(businessId, criteria, page,
				size);
		List<UUID> pageIds = employees.content().stream().map(Employee::id).toList();
		// One lookup for the whole page, so the listing never queries services per employee.
		Map<UUID, List<EmployeeServiceRef>> services = pageIds.isEmpty() ? Map.of()
				: employeeServicesQuery.findActiveServicesOf(businessId, pageIds);
		List<EmployeeListItemResponse> content = employees.content()
			.stream()
			.map(employee -> toItem(employee, services.getOrDefault(employee.id(), List.of())))
			.toList();
		return new PageResponse<>(content, employees.page(), employees.size(), employees.totalElements(),
				employees.totalPages());
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

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static EmployeeListItemResponse toItem(Employee employee, List<EmployeeServiceRef> services) {
		return new EmployeeListItemResponse(employee.id(), employee.firstName(), employee.lastName(),
				employee.phone(), employee.email(), employee.status().name(), services);
	}

}
