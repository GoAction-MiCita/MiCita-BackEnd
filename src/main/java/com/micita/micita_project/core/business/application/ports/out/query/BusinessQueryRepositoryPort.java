package com.micita.micita_project.core.business.application.ports.out.query;

import java.util.UUID;

public interface BusinessQueryRepositoryPort {

	boolean existsByOwnerId(UUID ownerId);

}
