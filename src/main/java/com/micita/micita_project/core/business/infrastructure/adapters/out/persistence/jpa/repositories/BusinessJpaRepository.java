package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;

import com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.entities.BusinessEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BusinessJpaRepository extends JpaRepository<BusinessEntity, UUID> {

	boolean existsByOwnerId(UUID ownerId);

	@Query("select b.id from BusinessEntity b where b.ownerId = :ownerId")
	Optional<UUID> findIdByOwnerId(UUID ownerId);

}
