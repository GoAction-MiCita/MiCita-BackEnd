package com.micita.micita_project.core.business.application.ports.out.query;

import java.util.Optional;
import java.util.UUID;

public interface BusinessQueryRepositoryPort {

	boolean existsByOwnerId(UUID ownerId);

	Optional<UUID> findIdByOwnerId(UUID ownerId);

}
