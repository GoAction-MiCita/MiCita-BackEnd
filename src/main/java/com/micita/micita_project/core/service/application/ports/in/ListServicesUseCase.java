package com.micita.micita_project.core.service.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.application.dto.request.ServiceListFilter;
import com.micita.micita_project.core.service.application.dto.response.ServiceListItemResponse;

public interface ListServicesUseCase {

	/**
	 * Services of the caller's business that are not deleted, by name, with their employees (HU-08),
	 * narrowed by the optional {@code filter} (HU-13).
	 *
	 * @throws com.micita.micita_project.common.shared.exception.InvalidPageRequestException if {@code page},
	 * {@code size}, the name or the status is out of range
	 * @throws com.micita.micita_project.core.service.domain.exceptions.InvalidEmployeeIdsException if any
	 * employee in the filter is unknown, of another business or deleted
	 */
	PageResponse<ServiceListItemResponse> list(UUID ownerId, ServiceListFilter filter, int page, int size);

}
