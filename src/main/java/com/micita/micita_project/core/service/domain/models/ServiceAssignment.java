package com.micita.micita_project.core.service.domain.models;

import java.util.UUID;

/**
 * One row of {@code service_employees} joined with a non-deleted service: which employee provides
 * which service.
 */
public record ServiceAssignment(UUID employeeId, UUID serviceId, String serviceName) {

}
