package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.micita.micita_project.core.service.domain.models.ServiceAssignment;
import com.micita.micita_project.core.service.domain.models.ServiceStatus;
import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEmployeeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ServiceEmployeeJpaRepository extends JpaRepository<ServiceEmployeeEntity, ServiceEmployeeEntity.Key> {

	List<ServiceEmployeeEntity> findByEmployeeId(UUID employeeId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from ServiceEmployeeEntity se where se.employeeId = :employeeId")
	void deleteByEmployeeId(UUID employeeId);

	@Query("select new com.micita.micita_project.core.service.domain.models.ServiceAssignment(se.employeeId, s.id, s.name) "
			+ "from ServiceEmployeeEntity se, ServiceEntity s "
			+ "where s.id = se.serviceId and s.businessId = :businessId and se.employeeId in :employeeIds "
			+ "and s.status <> :excluded order by s.name, s.id")
	List<ServiceAssignment> findActiveAssignments(UUID businessId, Collection<UUID> employeeIds,
			ServiceStatus excluded);

}
