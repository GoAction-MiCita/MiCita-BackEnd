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
 * HU-10: the rules of HU-09, and employeeIds is required (empty is valid, missing is not).
 */
class UpdateServiceRequestValidationTest {

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
		assertThat(violations(new UpdateServiceRequest("Corte", null, null, null, "ACTIVE", List.of()))).isEmpty();
	}

	@Test
	void allFieldsIsValid() {
		assertThat(violations(new UpdateServiceRequest("Corte", "Clásico", 45, new BigDecimal("35000.50"), "INACTIVE",
				List.of(UUID.randomUUID()))))
			.isEmpty();
	}

	@Test
	void missingEmployeeListIsRejectedSoItCannotWipeTheAssignments() {
		assertThat(violations(new UpdateServiceRequest("Corte", null, null, null, "ACTIVE", null)))
			.containsExactly("employeeIds");
	}

	@Test
	void nullEmployeeIdIsRejected() {
		assertThat(violations(new UpdateServiceRequest("Corte", null, null, null, "ACTIVE",
				Arrays.asList(UUID.randomUUID(), null))))
			.hasSize(1);
	}

	@Test
	void blankNameIsRejected() {
		assertThat(violations(new UpdateServiceRequest("  ", null, null, null, "ACTIVE", List.of())))
			.containsExactly("name");
	}

	@Test
	void nameAndDescriptionHaveMaximumLengths() {
		assertThat(violations(new UpdateServiceRequest("x".repeat(151), "y".repeat(256), null, null, "ACTIVE",
				List.of())))
			.containsExactlyInAnyOrder("name", "description");
	}

	@Test
	void statusMustBeActiveOrInactive() {
		for (String status : Arrays.asList(null, "DELETED", "active", "")) {
			assertThat(violations(new UpdateServiceRequest("Corte", null, null, null, status, List.of())))
				.as(String.valueOf(status))
				.containsExactly("status");
		}
	}

	@Test
	void durationAndPriceRules() {
		assertThat(violations(new UpdateServiceRequest("Corte", null, 0, new BigDecimal("-1"), "ACTIVE", List.of())))
			.containsExactlyInAnyOrder("durationMinutes", "price");
		assertThat(violations(new UpdateServiceRequest("Corte", null, null, new BigDecimal("10.123"), "ACTIVE",
				List.of())))
			.containsExactly("price");
		assertThat(violations(new UpdateServiceRequest("Corte", null, null, BigDecimal.ZERO, "ACTIVE", List.of())))
			.isEmpty();
	}

	private static Set<String> violations(UpdateServiceRequest request) {
		return validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(path -> path.toString().replaceAll("\\[.*", ""))
			.collect(Collectors.toSet());
	}

}
