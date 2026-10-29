package com.micita.micita_project.core.employee.infrastructure.adapters.out.persistence.jpa.entities;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.employee.domain.models.EmployeeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Email and phone are unique per business among non-deleted employees. That needs partial unique
 * indexes, which wait for migrations (D-23); until then the use case checks it and these indexes only
 * speed up the lookups.
 */
@Entity
@Table(name = "employees",
		indexes = { @Index(name = "idx_employees_business_email", columnList = "business_id, email"),
				@Index(name = "idx_employees_business_phone", columnList = "business_id, phone") })
@Getter
@Setter
public class EmployeeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "business_id", nullable = false)
	private UUID businessId;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(length = 16)
	private String phone;

	@Column(length = 254)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EmployeeStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}
