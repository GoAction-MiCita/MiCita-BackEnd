package com.micita.micita_project.core.business.application.command;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.micita.micita_project.core.business.application.dto.request.CreateBusinessRequest;
import com.micita.micita_project.core.business.application.dto.response.BusinessResponse;
import com.micita.micita_project.core.business.application.mappers.BusinessMapper;
import com.micita.micita_project.core.business.application.ports.in.CreateBusinessUseCase;
import com.micita.micita_project.core.business.application.ports.out.command.BusinessCommandRepositoryPort;
import com.micita.micita_project.core.business.application.ports.out.query.BusinessQueryRepositoryPort;
import com.micita.micita_project.core.business.domain.exceptions.BusinessAlreadyExistsException;
import com.micita.micita_project.core.business.domain.models.Business;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateBusinessCommandHandler implements CreateBusinessUseCase {

	private final BusinessQueryRepositoryPort businessQueryRepository;

	private final BusinessCommandRepositoryPort businessCommandRepository;

	private final BusinessMapper businessMapper;

	private final Clock clock;

	@Override
	public BusinessResponse create(UUID ownerId, CreateBusinessRequest request) {
		// One business per user in the MVP.
		if (businessQueryRepository.existsByOwnerId(ownerId)) {
			throw new BusinessAlreadyExistsException();
		}
		String email = optional(request.email());
		Business business = Business.register(ownerId, request.name().trim(), optional(request.taxId()),
				request.address().trim(), request.phone(), email == null ? null : email.toLowerCase(Locale.ROOT),
				Instant.now(clock));
		return businessMapper.toResponse(businessCommandRepository.save(business));
	}

	/** Optional fields sent blank are stored as absent. */
	private static String optional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
