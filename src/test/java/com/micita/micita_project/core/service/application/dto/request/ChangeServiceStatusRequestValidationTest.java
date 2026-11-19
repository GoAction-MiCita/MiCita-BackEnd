package com.micita.micita_project.core.service.application.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * HU-11: only ACTIVE and INACTIVE are valid; DELETED belongs to HU-14.
 */
class ChangeServiceStatusRequestValidationTest {

	private static ValidatorFactory factory;

	private static Validator validator;

	@BeforeAll
	static void setUp() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		factory.close();
	}

	@ParameterizedTest
	@ValueSource(strings = { "ACTIVE", "INACTIVE" })
	void activeAndInactiveAreValid(String status) {
		assertThat(validator.validate(new ChangeServiceStatusRequest(status))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "DELETED", "active", "PAUSED", "" })
	void deletedAndInvalidValuesAreRejected(String status) {
		assertThat(validator.validate(new ChangeServiceStatusRequest(status))).hasSize(1);
	}

	@Test
	void missingStatusIsRejected() {
		assertThat(validator.validate(new ChangeServiceStatusRequest(null))).hasSize(1);
	}

}
