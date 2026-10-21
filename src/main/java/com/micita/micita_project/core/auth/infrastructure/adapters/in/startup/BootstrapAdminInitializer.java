package com.micita.micita_project.core.auth.infrastructure.adapters.in.startup;

import com.micita.micita_project.common.shared.security.BootstrapUserProperties;
import com.micita.micita_project.core.auth.application.ports.in.BootstrapAdminUseCase;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

	private final BootstrapAdminUseCase bootstrapAdminUseCase;

	private final BootstrapUserProperties bootstrapUser;

	public BootstrapAdminInitializer(BootstrapAdminUseCase bootstrapAdminUseCase,
			BootstrapUserProperties bootstrapUser) {
		this.bootstrapAdminUseCase = bootstrapAdminUseCase;
		this.bootstrapUser = bootstrapUser;
	}

	@Override
	public void run(ApplicationArguments args) {
		bootstrapAdminUseCase.ensureAdmin(bootstrapUser.username(), bootstrapUser.password());
	}

}
