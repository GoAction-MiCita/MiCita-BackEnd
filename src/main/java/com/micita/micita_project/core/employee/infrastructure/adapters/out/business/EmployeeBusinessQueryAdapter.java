package com.micita.micita_project.core.employee.infrastructure.adapters.out.business;

import java.util.UUID;

import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.employee.application.ports.out.query.BusinessQueryPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the caller's business through the business module, never from a request parameter.
 */
@Component
@RequiredArgsConstructor
public class EmployeeBusinessQueryAdapter implements BusinessQueryPort {

	private final BusinessOwnershipQueryUseCase businessOwnershipQuery;

	@Override
	public UUID requireBusinessIdOf(UUID userId) {
		return businessOwnershipQuery.requireBusinessIdOf(userId);
	}

}
