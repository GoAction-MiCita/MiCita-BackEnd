package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;

import com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities.ServiceEmployeeEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ServiceEmployeeJpaRepository extends JpaRepository<ServiceEmployeeEntity, ServiceEmployeeEntity.Key> {

	List<ServiceEmployeeEntity> findByEmployeeId(UUID employeeId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from ServiceEmployeeEntity se where se.employeeId = :employeeId")
	void deleteByEmployeeId(UUID employeeId);

}
