package com.micita.micita_project.core.service.infrastructure.adapters.out.appointment;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.service.application.ports.out.query.FutureAppointmentsQueryPort;

import org.springframework.stereotype.Component;

/**
 * Provisional F1 adapter: there are no appointments until the appointment module and its Firestore
 * adapter exist (F2, ADR-001), so no service has future ones. Replace it then.
 */
@Component
public class NoFutureServiceAppointmentsAdapter implements FutureAppointmentsQueryPort {

	@Override
	public long countFutureAppointmentsOf(UUID businessId, UUID serviceId, Instant from) {
		return 0;
	}

}
