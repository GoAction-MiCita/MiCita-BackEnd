package com.micita.micita_project.core.employee.application.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * HU-18: the same rules as registration (HU-17).
 */
class UpdateEmployeeRequestValidationTest {

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

	@Test
	void completeRequestIsValid() {
		assertThat(validator.validate(new UpdateEmployeeRequest("Carlos", "Gómez", "+573001234567",
				"carlos@elcorte.com", "INACTIVE", List.of(UUID.randomUUID())))).isEmpty();
	}

	@Test
	void optionalFieldsMayBeMissingOrEmpty() {
		assertThat(validator.validate(new UpdateEmployeeRequest("Carlos", "Gómez", null, null, "ACTIVE", null)))
			.isEmpty();
		assertThat(validator.validate(new UpdateEmployeeRequest("Carlos", "Gómez", "", "", "ACTIVE", List.of())))
			.isEmpty();
	}

	@Test
	void invalidFieldsAreReportedOneByOne() {
		assertThat(fields(validator.validate(new UpdateEmployeeRequest(" ", "x".repeat(101), "12-34", "bad", null,
				Arrays.asList((UUID) null)))))
			.containsExactlyInAnyOrder("firstName", "lastName", "phone", "email", "status", "serviceIds");
	}

	@ParameterizedTest
	@ValueSource(strings = { "DELETED", "active", "PAUSED" })
	void statusOtherThanActiveOrInactiveIsRejected(String status) {
		assertThat(fields(validator.validate(new UpdateEmployeeRequest("Carlos", "Gómez", null, null, status, null))))
			.containsExactly("status");
	}

	private static List<String> fields(java.util.Set<ConstraintViolation<UpdateEmployeeRequest>> violations) {
		// A list element is reported as "serviceIds[0].<list element>": keep the property name only.
		return violations.stream().map(violation -> violation.getPropertyPath().toString().split("\\[")[0]).toList();
	}

}
