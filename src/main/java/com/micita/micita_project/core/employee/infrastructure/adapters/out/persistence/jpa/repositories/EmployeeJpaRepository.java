package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities.EmployeeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeJpaRepository
		extends JpaRepository<EmployeeEntity, UUID>, JpaSpecificationExecutor<EmployeeEntity> {

	boolean existsByBusinessIdAndEmailAndStatusNot(UUID businessId, String email, EmployeeStatus status);

	boolean existsByBusinessIdAndPhoneAndStatusNot(UUID businessId, String phone, EmployeeStatus status);

	boolean existsByBusinessIdAndEmailAndStatusNotAndIdNot(UUID businessId, String email, EmployeeStatus status,
			UUID id);

	boolean existsByBusinessIdAndPhoneAndStatusNotAndIdNot(UUID businessId, String phone, EmployeeStatus status,
			UUID id);

	Optional<EmployeeEntity> findByIdAndBusinessIdAndStatusNot(UUID id, UUID businessId, EmployeeStatus status);

	@Query("select e.id from EmployeeEntity e where e.businessId = :businessId and e.id in :ids and e.status <> :excluded")
	Set<UUID> findIdsOfBusiness(UUID businessId, Collection<UUID> ids, EmployeeStatus excluded);

	@Query("select e.id from EmployeeEntity e where e.businessId = :businessId and e.id in :ids and e.status = :status")
	Set<UUID> findIdsOfBusinessWithStatus(UUID businessId, Collection<UUID> ids, EmployeeStatus status);

}
