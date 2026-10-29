package com.micita.micita_project.core.service.infrastructure.adapters.out.persistence.jpa.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.service.domain.models.ServiceStatus;
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
 * Columns follow HU-09. So far only the lookups that assign services to employees (HU-17) use it.
 */
@Entity
@Table(name = "services", indexes = @Index(name = "idx_services_business_id", columnList = "business_id"))
@Getter
@Setter
public class ServiceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "business_id", nullable = false)
	private UUID businessId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 255)
	private String description;

	@Column(name = "duration_minutes")
	private Integer durationMinutes;

	@Column(precision = 12, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ServiceStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

}
