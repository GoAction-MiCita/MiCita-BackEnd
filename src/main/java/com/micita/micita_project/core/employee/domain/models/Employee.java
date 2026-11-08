package com.micita.micita_project.core.employee.domain.models;

import java.time.Instant;
import java.util.UUID;

/**
 * An employee of a business. {@code phone} and {@code email} are optional; the email is stored
 * lowercased. A {@code null} id means the employee has not been persisted yet.
 */
public record Employee(UUID id, UUID businessId, String firstName, String lastName, String phone, String email,
		EmployeeStatus status, Instant createdAt, Instant updatedAt) {

	public static Employee register(UUID businessId, String firstName, String lastName, String phone, String email,
			EmployeeStatus status, Instant now) {
		return new Employee(null, businessId, firstName, lastName, phone, email, status, now, now);
	}

	/**
	 * @return a copy with {@code status} and {@code updatedAt} changed; everything else, including the id
	 * and the creation time, is kept
	 */
	public Employee withStatus(EmployeeStatus status, Instant now) {
		return new Employee(id, businessId, firstName, lastName, phone, email, status, createdAt, now);
	}

	/**
	 * @return a copy with the editable data and {@code updatedAt} changed; the id, the business and the
	 * creation time are kept
	 */
	public Employee withDetails(String firstName, String lastName, String phone, String email, EmployeeStatus status,
			Instant now) {
		return new Employee(id, businessId, firstName, lastName, phone, email, status, createdAt, now);
	}

}
