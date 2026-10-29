package com.micita.micita_project.core.service.application.ports.out.command;

import java.util.Set;
import java.util.UUID;

public interface ServiceEmployeeCommandRepositoryPort {

	void replaceServicesOfEmployee(UUID employeeId, Set<UUID> serviceIds);

}
