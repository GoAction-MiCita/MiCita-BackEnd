package com.micita.micita_project.core.service.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A service a business offers. {@code description}, {@code durationMinutes} and {@code price} (COP) are
 * optional. A {@code null} id means the service has not been persisted yet.
 */
public record Service(UUID id, UUID businessId, String name, String description, Integer durationMinutes,
		BigDecimal price, ServiceStatus status, Instant createdAt, Instant updatedAt) {

	public static Service register(UUID businessId, String name, String description, Integer durationMinutes,
			BigDecimal price, ServiceStatus status, Instant now) {
		return new Service(null, businessId, name, description, durationMinutes, price, status, now, now);
	}

}
