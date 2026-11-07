package com.micita.micita_project.core.employee.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.application.dto.request.EmployeeListFilter;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeListItemResponse;

public interface ListEmployeesUseCase {

	/**
	 * Lists the employees of the owner's business that match {@code filter}, newest first, without
	 * deleted employees. Paging and totals count only the matching employees.
	 * @throws com.micita.micita_project.common.shared.exception.InvalidPageRequestException if
	 * {@code page} is negative, {@code size} is outside 1..50, {@code name} is longer than 100 characters
	 * or {@code status} is not {@code ACTIVE} or {@code INACTIVE}
	 * @throws com.micita.micita_project.common.shared.exception.InvalidReferencesException if a service in
	 * {@code serviceIds} is unknown, of another business or deleted
	 */
	PageResponse<EmployeeListItemResponse> list(UUID ownerId, EmployeeListFilter filter, int page, int size);

}
