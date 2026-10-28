package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories;

import java.util.UUID;

import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.entities.BusinessEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessJpaRepository extends JpaRepository<BusinessEntity, UUID> {

	boolean existsByOwnerId(UUID ownerId);

}
