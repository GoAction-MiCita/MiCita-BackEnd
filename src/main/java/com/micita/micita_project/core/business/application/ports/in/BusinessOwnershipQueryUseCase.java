package com.micita.micita_project.core.business.application.ports.in;

import java.util.UUID;

public interface BusinessOwnershipQueryUseCase {

	boolean ownsBusiness(UUID userId);

}
