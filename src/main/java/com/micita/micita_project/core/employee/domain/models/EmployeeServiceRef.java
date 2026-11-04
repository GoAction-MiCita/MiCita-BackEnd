package com.micita.micita_project.core.employee.domain.models;

import java.util.UUID;

/**
 * A service an employee provides, as shown in the employee listing.
 */
public record EmployeeServiceRef(UUID id, String name) {

}
