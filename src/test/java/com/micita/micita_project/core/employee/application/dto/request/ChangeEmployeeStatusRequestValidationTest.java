package com.micita.micita_project.core.employee.application.dto.request;

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
 * HU-20: only ACTIVE and INACTIVE are accepted; DELETED is HU-21.
 */
class ChangeEmployeeStatusRequestValidationTest {

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
		assertThat(validator.validate(new ChangeEmployeeStatusRequest(status))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "DELETED", "active", "PAUSED", "" })
	void otherValuesAreRejected(String status) {
		assertThat(validator.validate(new ChangeEmployeeStatusRequest(status))).hasSize(1);
	}

	@Test
	void missingStatusIsRejected() {
		assertThat(validator.validate(new ChangeEmployeeStatusRequest(null))).hasSize(1);
	}

}
