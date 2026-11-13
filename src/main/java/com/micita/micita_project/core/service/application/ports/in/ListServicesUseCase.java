package com.micita.micita_project.core.service.application.ports.in;

import java.util.UUID;

import com.micita.micita_project.common.shared.pagination.PageResponse;
import com.micita.micita_project.core.service.application.dto.response.ServiceListItemResponse;

public interface ListServicesUseCase {

	/**
	 * Services of the caller's business that are not deleted, by name, with their employees (HU-08).
	 *
	 * @throws com.micita.micita_project.common.shared.exception.InvalidPageRequestException if {@code page} or
	 * {@code size} is out of range
	 */
	PageResponse<ServiceListItemResponse> list(UUID ownerId, int page, int size);

}
