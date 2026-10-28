package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.command;

import com.micita.micita_project.core.business.application.ports.out.command.BusinessCommandRepositoryPort;
import com.micita.micita_project.core.business.domain.exceptions.BusinessAlreadyExistsException;
import com.micita.micita_project.core.business.domain.models.Business;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.mappers.BusinessBDMapper;
import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories.BusinessJpaRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class BusinessCommandJpaAdapter implements BusinessCommandRepositoryPort {

	private final BusinessJpaRepository businessJpaRepository;

	private final BusinessBDMapper businessBDMapper;

	public BusinessCommandJpaAdapter(BusinessJpaRepository businessJpaRepository, BusinessBDMapper businessBDMapper) {
		this.businessJpaRepository = businessJpaRepository;
		this.businessBDMapper = businessBDMapper;
	}

	@Override
	public Business save(Business business) {
		try {
			// Flush so a unique-constraint violation surfaces here, not at commit time.
			return businessBDMapper.toDomain(businessJpaRepository.saveAndFlush(businessBDMapper.toEntity(business)));
		}
		catch (DataIntegrityViolationException ex) {
			// A concurrent onboarding of the same user won after the existence check.
			throw new BusinessAlreadyExistsException();
		}
	}

}
