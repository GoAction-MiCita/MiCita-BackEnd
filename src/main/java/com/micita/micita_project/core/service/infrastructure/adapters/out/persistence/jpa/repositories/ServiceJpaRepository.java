package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ServiceJpaRepository extends JpaRepository<ServiceEntity, UUID>, JpaSpecificationExecutor<ServiceEntity> {

	@Query("select s.id from ServiceEntity s where s.businessId = :businessId and s.id in :ids and s.status <> :excluded")
	Set<UUID> findIdsOfBusiness(UUID businessId, Collection<UUID> ids, ServiceStatus excluded);

	@Query("select count(s) > 0 from ServiceEntity s where s.businessId = :businessId "
			+ "and lower(s.name) = lower(:name) and s.status <> :excluded")
	boolean existsByName(UUID businessId, String name, ServiceStatus excluded);

}
