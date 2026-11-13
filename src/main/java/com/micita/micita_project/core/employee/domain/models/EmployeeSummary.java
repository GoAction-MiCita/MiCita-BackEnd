package com.micita.micita_project.core.employee.domain.models;

import java.util.UUID;

/**
 * The part of an employee other modules may show (ADR-002): who it is, nothing else.
 */
public record EmployeeSummary(UUID id, String firstName, String lastName) {

}
