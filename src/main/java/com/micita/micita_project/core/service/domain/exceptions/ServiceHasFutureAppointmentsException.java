package com.micita.micita_project.core.service.domain.exceptions;

import java.util.Map;

import com.micita.micita_project.common.shared.exception.ConflictException;

import lombok.Getter;

/**
 * The service still has future appointments that occupy the agenda, so it can't be deleted; it can be
 * deactivated instead. The count goes in the {@code futureAppointments} member of the 409.
 */
@Getter
public class ServiceHasFutureAppointmentsException extends ConflictException {

	private final long futureAppointments;

	public ServiceHasFutureAppointmentsException(long futureAppointments) {
		super("The service has " + futureAppointments
				+ " future appointments; deactivate it instead or reassign them first",
				Map.of("futureAppointments", futureAppointments));
		this.futureAppointments = futureAppointments;
	}

}
