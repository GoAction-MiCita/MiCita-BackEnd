package com.micita.micita_project.core.auth.application.dto.request;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegisterUserRequestValidationTest {

	private static ValidatorFactory validatorFactory;

	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void validRequestHasNoViolations() {
		assertThat(validator.validate(withPassword("Secret#1"))).isEmpty();
	}

	@Test
	void blankRequiredFieldsAreReportedPerField() {
		RegisterUserRequest request = new RegisterUserRequest("", " ", "", "", "", "", "");

		assertThat(invalidFields(request)).containsExactlyInAnyOrder("firstName", "lastName", "address", "phone",
				"email", "username", "password");
	}

	@Test
	void malformedEmailAndPhoneAreRejected() {
		RegisterUserRequest request = new RegisterUserRequest("Ana", "Pérez", "Calle 10", "300-ABC", "not-an-email",
				"ana.perez", "Secret#1");

		assertThat(invalidFields(request)).containsExactlyInAnyOrder("email", "phone");
	}

	@ParameterizedTest
	@ValueSource(strings = { "Se#1", "secret#1", "SECRET#1", "Secret12", "Secret 1" })
	void weakPasswordsAreRejected(String password) {
		assertThat(invalidFields(withPassword(password))).containsExactly("password");
	}

	@Test
	void passwordLongerThanBcryptLimitIsRejected() {
		assertThat(invalidFields(withPassword("Aa#" + "x".repeat(70)))).containsExactly("password");
	}

	private static RegisterUserRequest withPassword(String password) {
		return new RegisterUserRequest("Ana", "Pérez", "Calle 10", "+573001234567", "ana@example.com", "ana.perez",
				password);
	}

	private static Set<String> invalidFields(RegisterUserRequest request) {
		return validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(Object::toString)
			.collect(Collectors.toSet());
	}

}
