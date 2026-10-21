package com.micita.micita_project.core.auth.infrastructure.adapters.out.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PendingBusinessQueryAdapterTest {

	@Test
	void nobodyOwnsABusinessUntilOnboardingExists() {
		assertThat(new PendingBusinessQueryAdapter().existsForOwner(UUID.randomUUID())).isFalse();
	}

}
