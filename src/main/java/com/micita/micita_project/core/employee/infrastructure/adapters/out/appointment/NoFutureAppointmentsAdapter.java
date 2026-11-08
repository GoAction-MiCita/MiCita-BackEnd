package com.micita.micita_project.core.employee.infrastructure.adapters.out.appointment;

import java.time.Instant;
import java.util.UUID;

import com.micita.micita_project.core.employee.application.ports.out.query.FutureAppointmentsQueryPort;

import org.springframework.stereotype.Component;

/**
 * Provisional F1 adapter: there are no appointments until the appointment module and its Firestore
 * adapter exist (F2, ADR-001), so no employee has future ones. Replace it then.
 */
@Component
public class NoFutureAppointmentsAdapter implements FutureAppointmentsQueryPort {

	@Override
	public long countFutureAppointmentsOf(UUID businessId, UUID employeeId, Instant from) {
		return 0;
	}

}
