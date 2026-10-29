package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Which employees can provide which service (ADR-002). Rows are kept when either side is soft
 * deleted; queries skip the deleted ones.
 */
@Entity
@IdClass(ServiceEmployeeEntity.Key.class)
@Table(name = "service_employees",
		indexes = @Index(name = "idx_service_employees_employee_id", columnList = "employee_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ServiceEmployeeEntity {

	@Id
	@Column(name = "service_id", nullable = false)
	private UUID serviceId;

	@Id
	@Column(name = "employee_id", nullable = false)
	private UUID employeeId;

	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	public static class Key implements Serializable {

		private UUID serviceId;

		private UUID employeeId;

	}

}
