package com.micita.micita_project.core.business.domain.models;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * A business owned by one user (one per user in the MVP). {@code taxId} (NIT) and {@code email}
 * are optional. A {@code null} id means the business has not been persisted yet.
 */
public record Business(UUID id, UUID ownerId, String name, String taxId, String address, String phone, String email,
		BusinessStatus status, ZoneId timeZone, Instant createdAt) {

	/** Appointment dates are computed in this zone unless the business sets another one (ADR-001). */
	public static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("America/Bogota");

	/**
	 * A new business, active and in the default time zone, ready to be configured.
	 */
	public static Business register(UUID ownerId, String name, String taxId, String address, String phone,
			String email, Instant now) {
		return new Business(null, ownerId, name, taxId, address, phone, email, BusinessStatus.ACTIVE,
				DEFAULT_TIME_ZONE, now);
	}

}
