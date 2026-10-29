package com.micita.micita_project.core.service.domain.models;

/**
 * Services are never removed: {@code DELETED} is a soft delete, so appointments in Firestore keep
 * pointing at them (ADR-001).
 */
public enum ServiceStatus {

	ACTIVE, INACTIVE, DELETED

}
