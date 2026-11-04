package com.micita.micita_project.core.employee.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.employee.application.dto.response.EmployeeListItemResponse;

public interface ListEmployeesUseCase {

	/**
	 * Lists the employees of the owner's business, newest first, without deleted employees.
	 * @throws com.micita.micita_project.common.shared.exception.InvalidPageRequestException if
	 * {@code page} is negative or {@code size} is outside 1..50
	 */
	PageResponse<EmployeeListItemResponse> list(UUID ownerId, int page, int size);

}
