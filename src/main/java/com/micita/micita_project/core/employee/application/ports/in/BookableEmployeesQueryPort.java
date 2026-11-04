package com.micita.micita_project.core.employee.application.ports.in;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Which employees can take appointments (F2). Only {@code ACTIVE} ones are bookable: inactive and
 * deleted employees never are.
 */
public interface BookableEmployeesQueryPort {

	/**
	 * @return the ids among {@code employeeIds} that belong to the business and are {@code ACTIVE}
	 */
	Set<UUID> findBookableIds(UUID businessId, Collection<UUID> employeeIds);

}
