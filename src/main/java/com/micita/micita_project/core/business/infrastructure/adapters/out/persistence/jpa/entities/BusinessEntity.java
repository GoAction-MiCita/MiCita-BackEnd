package com.micita.micita_project.core.business.infrastructure.adapters.out.persistence.jpa.entities;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.business.domain.models.BusinessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "businesses")
@Getter
@Setter
public class BusinessEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// Unique: one business per user in the MVP, also under concurrent onboarding requests.
	@Column(name = "owner_id", nullable = false, unique = true)
	private UUID ownerId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(name = "tax_id", length = 30)
	private String taxId;

	@Column(nullable = false)
	private String address;

	@Column(nullable = false, length = 16)
	private String phone;

	@Column(length = 254)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BusinessStatus status;

	@Column(name = "time_zone", nullable = false, length = 64)
	private String timeZone;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

}
