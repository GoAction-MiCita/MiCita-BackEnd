package com.micita.micita_project.core.service.domain.models;

import java.util.UUID;

/**
 * An employee as the service module shows it: who it is, as the employee module reports it.
 */
public record ServiceEmployee(UUID id, String firstName, String lastName) {

}
