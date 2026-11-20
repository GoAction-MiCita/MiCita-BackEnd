package com.micita.micita_project.core.service.infrastructure.adapters.out.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * F1: without the appointment module there are no appointments, so no service has future ones.
 */
class NoFutureServiceAppointmentsAdapterTest {

	@Test
	void reportsNoFutureAppointments() {
		assertThat(new NoFutureServiceAppointmentsAdapter().countFutureAppointmentsOf(UUID.randomUUID(),
				UUID.randomUUID(), Instant.now())).isZero();
	}

}
