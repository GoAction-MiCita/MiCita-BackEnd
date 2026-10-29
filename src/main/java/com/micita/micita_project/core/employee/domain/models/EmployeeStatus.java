package com.micita.micita_project.core.employee.domain.models;

/**
 * Employees are never removed: {@code DELETED} is a soft delete, so appointments in Firestore keep
 * pointing at them (ADR-001). Only {@code ACTIVE} and {@code INACTIVE} can be chosen on registration.
 */
public enum EmployeeStatus {

	ACTIVE, INACTIVE, DELETED

}
