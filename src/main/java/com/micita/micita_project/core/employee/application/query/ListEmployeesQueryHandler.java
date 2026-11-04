package com.micita.micita_project.core.employee.application.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.micita.micita_project.common.shared.exception.InvalidPageRequestException;
import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeListItemResponse;
import com.micita.micita_project.core.employee.application.ports.in.ListEmployeesUseCase;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeQueryRepositoryPort;
import com.micita.micita_project.core.employee.application.ports.out.query.EmployeeServicesQueryPort;
import com.micita.micita_project.core.employee.domain.models.Employee;
import com.micita.micita_project.core.employee.domain.models.EmployeeServiceRef;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListEmployeesQueryHandler implements ListEmployeesUseCase {

	static final int MAX_PAGE_SIZE = 50;

	private final BusinessQueryPort businessQuery;

	private final EmployeeQueryRepositoryPort employeeQueryRepository;

	private final EmployeeServicesQueryPort employeeServicesQuery;

	@Override
	public PageResponse<EmployeeListItemResponse> list(UUID ownerId, int page, int size) {
		validate(page, size);
		UUID businessId = businessQuery.requireBusinessIdOf(ownerId);
		PageResponse<Employee> employees = employeeQueryRepository.findPageOfBusiness(businessId, page, size);
		List<UUID> employeeIds = employees.content().stream().map(Employee::id).toList();
		// One lookup for the whole page, so the listing never queries services per employee.
		Map<UUID, List<EmployeeServiceRef>> services = employeeIds.isEmpty() ? Map.of()
				: employeeServicesQuery.findActiveServicesOf(businessId, employeeIds);
		List<EmployeeListItemResponse> content = employees.content()
			.stream()
			.map(employee -> toItem(employee, services.getOrDefault(employee.id(), List.of())))
			.toList();
		return new PageResponse<>(content, employees.page(), employees.size(), employees.totalElements(),
				employees.totalPages());
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

	private static EmployeeListItemResponse toItem(Employee employee, List<EmployeeServiceRef> services) {
		return new EmployeeListItemResponse(employee.id(), employee.firstName(), employee.lastName(),
				employee.phone(), employee.email(), employee.status().name(), services);
	}

}
