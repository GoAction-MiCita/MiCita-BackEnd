package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Which employees can provide which service (ADR-002). Rows are kept when either side is soft
 * deleted; queries skip the deleted ones.
 */
@Entity
@IdClass(ServiceEmployeeEntity.Key.class)
@Table(name = "service_employees",
		indexes = @Index(name = "idx_service_employees_employee_id", columnList = "employee_id"))
public class ServiceEmployeeEntity {

	@Id
	@Column(name = "service_id", nullable = false)
	private UUID serviceId;

	@Id
	@Column(name = "employee_id", nullable = false)
	private UUID employeeId;

	protected ServiceEmployeeEntity() {
	}

	public ServiceEmployeeEntity(UUID serviceId, UUID employeeId) {
		this.serviceId = serviceId;
		this.employeeId = employeeId;
	}

	public UUID getServiceId() {
		return serviceId;
	}

	public UUID getEmployeeId() {
		return employeeId;
	}

	public static class Key implements Serializable {

		private UUID serviceId;

		private UUID employeeId;

		public Key() {
		}

		public Key(UUID serviceId, UUID employeeId) {
			this.serviceId = serviceId;
			this.employeeId = employeeId;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Key key && Objects.equals(serviceId, key.serviceId)
					&& Objects.equals(employeeId, key.employeeId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(serviceId, employeeId);
		}

	}

}
