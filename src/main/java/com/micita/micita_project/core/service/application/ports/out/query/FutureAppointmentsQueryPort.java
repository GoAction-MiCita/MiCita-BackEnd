package com.micita.micita_project.core.service.application.ports.out.query;

import java.time.Instant;
import java.util.UUID;

/**
 * Appointments live in Firestore (ADR-001). Until the appointment module exists (F2) a provisional
 * adapter answers that there are none.
 */
public interface FutureAppointmentsQueryPort {

	/**
	 * @return how many appointments of the service start at or after {@code from} and still occupy the
	 * agenda ({@code PENDING}, {@code CONFIRMED}, {@code IN_PROGRESS})
	 */
	long countFutureAppointmentsOf(UUID businessId, UUID serviceId, Instant from);

}
