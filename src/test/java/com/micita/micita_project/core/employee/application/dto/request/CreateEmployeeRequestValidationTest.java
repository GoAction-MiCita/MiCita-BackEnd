package com.micita.micita_project.core.employee.application.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * HU-17: first name, last name and status are required; phone, email and serviceIds are optional.
 */
class CreateEmployeeRequestValidationTest {

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
	void requiredFieldsOnlyIsValid() {
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", null, null, "ACTIVE", null))).isEmpty();
	}

	@Test
	void allFieldsIsValid() {
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", "3001234567", "carlos@elcorte.com",
				"INACTIVE", List.of(UUID.randomUUID()))))
			.isEmpty();
	}

	@Test
	void emptyPhoneMeansNoPhone() {
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", "", "", "ACTIVE", List.of()))).isEmpty();
	}

	@Test
	void blankNamesAreRejected() {
		assertThat(violations(new CreateEmployeeRequest(" ", "", null, null, "ACTIVE", null)))
			.containsExactlyInAnyOrder("firstName", "lastName");
	}

	@Test
	void namesLongerThan100CharactersAreRejected() {
		assertThat(violations(new CreateEmployeeRequest("a".repeat(101), "b".repeat(101), null, null, "ACTIVE", null)))
			.containsExactlyInAnyOrder("firstName", "lastName");
		assertThat(violations(new CreateEmployeeRequest("a".repeat(100), "b".repeat(100), null, null, "ACTIVE", null)))
			.isEmpty();
	}

	@Test
	void invalidPhoneAndEmailAreRejected() {
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", "12-34", "not-an-email", "ACTIVE", null)))
			.containsExactlyInAnyOrder("phone", "email");
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", "123456", null, "ACTIVE", null)))
			.containsExactly("phone");
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", "+1234567890123456", null, "ACTIVE", null)))
			.containsExactly("phone");
	}

	@Test
	void statusMustBeActiveOrInactive() {
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", null, null, null, null)))
			.containsExactly("status");
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", null, null, "DELETED", null)))
			.containsExactly("status");
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", null, null, "active", null)))
			.containsExactly("status");
	}

	@Test
	void nullServiceIdIsRejected() {
		assertThat(violations(new CreateEmployeeRequest("Carlos", "Gómez", null, null, "ACTIVE",
				Arrays.asList(UUID.randomUUID(), null))))
			.containsExactly("serviceIds[1].<list element>");
	}

	private static Set<String> violations(CreateEmployeeRequest request) {
		return validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(Object::toString)
			.collect(Collectors.toSet());
	}

}
