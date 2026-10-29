package com.micita.micita_project.core.business.infrastructure.adapters.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Test;

/**
 * HU-04: Swagger shows the BUSINESS_REQUIRED 403 on secured, non-exempt operations only.
 */
class BusinessRequiredOpenApiCustomizerTest {

	private final BusinessRequiredOpenApiCustomizer customizer = new BusinessRequiredOpenApiCustomizer();

	@Test
	void securedOperationsGetTheForbiddenResponse() {
		Operation services = operation();
		Operation admin = operation();
		admin.getResponses().addApiResponse("403", new ApiResponse().description("Not an admin"));

		customise(new Paths().addPathItem("/api/services", new PathItem().get(services))
			.addPathItem("/api/admin", new PathItem().get(admin)));

		assertThat(services.getResponses().get("403").getDescription()).contains("BUSINESS_REQUIRED");
		assertThat(services.getResponses().get("403").getContent().get("application/json").getSchema().get$ref())
			.isEqualTo("#/components/schemas/ProblemDetail");
		assertThat(admin.getResponses().get("403").getDescription()).isEqualTo("Not an admin");
	}

	@Test
	void exemptPublicAndNonApiOperationsAreLeftAlone() {
		Operation login = operation();
		Operation onboarding = operation();
		Operation publicBooking = operation();
		publicBooking.setSecurity(List.<SecurityRequirement>of());
		Operation outsideApi = operation();

		customise(new Paths().addPathItem("/api/auth/login", new PathItem().post(login))
			.addPathItem(BusinessController.ONBOARDING_PATH, new PathItem().post(onboarding))
			.addPathItem("/api/appointments", new PathItem().post(publicBooking))
			.addPathItem("/actuator/health", new PathItem().get(outsideApi)));

		assertThat(List.of(login, onboarding, publicBooking, outsideApi))
			.allSatisfy(op -> assertThat(op.getResponses()).doesNotContainKey("403"));
	}

	@Test
	void documentWithoutPathsIsIgnored() {
		OpenAPI openApi = new OpenAPI();

		customizer.customise(openApi);

		assertThat(openApi.getPaths()).isNull();
	}

	@Test
	void operationWithoutResponsesIsIgnored() {
		Operation operation = new Operation();

		customise(new Paths().addPathItem("/api/services", new PathItem().get(operation)));

		assertThat(operation.getResponses()).isNull();
	}

	private void customise(Paths paths) {
		customizer.customise(new OpenAPI().paths(paths));
	}

	private static Operation operation() {
		return new Operation().responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK")));
	}

}
