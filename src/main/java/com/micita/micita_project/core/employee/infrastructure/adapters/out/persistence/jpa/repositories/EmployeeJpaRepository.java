package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.repositories;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities.EmployeeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID> {

	boolean existsByBusinessIdAndEmailAndStatusNot(UUID businessId, String email, EmployeeStatus status);

	boolean existsByBusinessIdAndPhoneAndStatusNot(UUID businessId, String phone, EmployeeStatus status);

	@Query("select e.id from EmployeeEntity e where e.businessId = :businessId and e.id in :ids and e.status <> :excluded")
	Set<UUID> findIdsOfBusiness(UUID businessId, Collection<UUID> ids, EmployeeStatus excluded);

}
