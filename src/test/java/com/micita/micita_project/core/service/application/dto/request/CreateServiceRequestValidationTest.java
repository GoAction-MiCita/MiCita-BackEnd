package com.micita.micita_project.core.service.application.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
 * HU-09: name and status are required; description, duration, price and employeeIds are optional.
 */
class CreateServiceRequestValidationTest {

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
		assertThat(violations(new CreateServiceRequest("Corte", null, null, null, "ACTIVE", null))).isEmpty();
	}

	@Test
	void allFieldsIsValid() {
		assertThat(violations(new CreateServiceRequest("Corte", "Clásico", 45, new BigDecimal("35000.50"), "INACTIVE",
				List.of(UUID.randomUUID()))))
			.isEmpty();
	}

	@Test
	void zeroPriceIsValid() {
		assertThat(violations(new CreateServiceRequest("Cortesía", null, null, BigDecimal.ZERO, "ACTIVE", null)))
			.isEmpty();
	}

	@Test
	void blankNameIsRejected() {
		assertThat(violations(new CreateServiceRequest("  ", null, null, null, "ACTIVE", null))).containsExactly("name");
		assertThat(violations(new CreateServiceRequest(null, null, null, null, "ACTIVE", null))).containsExactly("name");
	}

	@Test
	void nameAndDescriptionHaveMaximumLengths() {
		assertThat(violations(new CreateServiceRequest("x".repeat(150), "y".repeat(255), null, null, "ACTIVE", null)))
			.isEmpty();
		assertThat(violations(new CreateServiceRequest("x".repeat(151), "y".repeat(256), null, null, "ACTIVE", null)))
			.containsExactlyInAnyOrder("name", "description");
	}

	@Test
	void missingStatusIsRejected() {
		assertThat(violations(new CreateServiceRequest("Corte", null, null, null, null, null)))
			.containsExactly("status");
	}

	@Test
	void statusOtherThanActiveOrInactiveIsRejected() {
		for (String status : List.of("DELETED", "active", "", "PAUSED")) {
			assertThat(violations(new CreateServiceRequest("Corte", null, null, null, status, null)))
				.as(status)
				.containsExactly("status");
		}
	}

	@Test
	void durationMustBePositive() {
		assertThat(violations(new CreateServiceRequest("Corte", null, 0, null, "ACTIVE", null)))
			.containsExactly("durationMinutes");
		assertThat(violations(new CreateServiceRequest("Corte", null, -5, null, "ACTIVE", null)))
			.containsExactly("durationMinutes");
	}

	@Test
	void priceCannotBeNegativeNorHaveMoreThanTwoDecimals() {
		assertThat(violations(new CreateServiceRequest("Corte", null, null, new BigDecimal("-0.01"), "ACTIVE", null)))
			.containsExactly("price");
		assertThat(violations(new CreateServiceRequest("Corte", null, null, new BigDecimal("10.123"), "ACTIVE", null)))
			.containsExactly("price");
	}

	@Test
	void nullEmployeeIdIsRejected() {
		assertThat(violations(new CreateServiceRequest("Corte", null, null, null, "ACTIVE",
				Arrays.asList(UUID.randomUUID(), null))))
			.hasSize(1);
	}

	private static Set<String> violations(CreateServiceRequest request) {
		return validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(path -> path.toString().replaceAll("\\[.*", ""))
			.collect(Collectors.toSet());
	}

}
