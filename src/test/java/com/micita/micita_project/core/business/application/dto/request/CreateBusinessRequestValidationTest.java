package com.micita.micita_project.core.business.application.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * HU-03: name, address and phone are required; NIT and email are optional.
 */
class CreateBusinessRequestValidationTest {

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
		assertThat(violations(new CreateBusinessRequest("El Corte", null, "Calle 10", "+573001234567", null)))
			.isEmpty();
	}

	@Test
	void allFieldsIsValid() {
		assertThat(violations(new CreateBusinessRequest("El Corte", "900123456-7", "Calle 10", "3001234567",
				"contacto@elcorte.com")))
			.isEmpty();
	}

	@Test
	void blankRequiredFieldsAreRejected() {
		assertThat(violations(new CreateBusinessRequest(" ", null, "", null, null)))
			.containsExactlyInAnyOrder("name", "address", "phone");
	}

	@Test
	void invalidPhoneAndEmailAreRejected() {
		assertThat(violations(new CreateBusinessRequest("El Corte", null, "Calle 10", "12-34", "not-an-email")))
			.containsExactlyInAnyOrder("phone", "email");
	}

	@Test
	void tooLongTaxIdIsRejected() {
		assertThat(violations(new CreateBusinessRequest("El Corte", "9".repeat(31), "Calle 10", "+573001234567", null)))
			.containsExactly("taxId");
	}

	private static Set<String> violations(CreateBusinessRequest request) {
		return validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(Object::toString)
			.collect(Collectors.toSet());
	}

}
