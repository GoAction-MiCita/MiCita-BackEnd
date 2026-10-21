package com.micita.micita_project.core.auth.infrastructure.adapters.in.startup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.micita.micita_project.common.shared.security.BootstrapUserProperties;
import com.micita.micita_project.core.auth.application.ports.in.BootstrapAdminUseCase;
import org.junit.jupiter.api.Test;

import org.springframework.boot.DefaultApplicationArguments;

class BootstrapAdminInitializerTest {

	@Test
	void runSeedsConfiguredAdmin() {
		BootstrapAdminUseCase useCase = mock(BootstrapAdminUseCase.class);
		BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(useCase,
				new BootstrapUserProperties("admin", "admin-pass"));

		initializer.run(new DefaultApplicationArguments());

		verify(useCase).ensureAdmin("admin", "admin-pass");
	}

}
