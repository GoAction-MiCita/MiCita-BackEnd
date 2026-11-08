package com.micita.micita_project.core.employee.infrastructure.adapters.out.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * F1: without the appointment module there are no appointments, so nobody has future ones.
 */
class NoFutureAppointmentsAdapterTest {

	@Test
	void reportsNoFutureAppointments() {
		assertThat(new NoFutureAppointmentsAdapter().countFutureAppointmentsOf(UUID.randomUUID(), UUID.randomUUID(),
				Instant.now())).isZero();
	}

}
