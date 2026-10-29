package com.micita.micita_project.core.business.infrastructure.adapters.in.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * Documents the HU-04 {@code BUSINESS_REQUIRED} 403 on every secured operation the
 * {@link BusinessRequiredInterceptor} guards, so controllers don't have to repeat it.
 */
@Component
public class BusinessRequiredOpenApiCustomizer implements OpenApiCustomizer {

	static final String FORBIDDEN = "403";

	static final String DESCRIPTION = "The user has no business yet (code BUSINESS_REQUIRED): complete "
			+ "POST /api/business/onboarding first";

	private final PathMatcher pathMatcher = new AntPathMatcher();

	@Override
	public void customise(OpenAPI openApi) {
		if (openApi.getPaths() == null) {
			return;
		}
		openApi.getPaths().forEach((path, item) -> {
			if (isGuarded(path)) {
				item.readOperations().stream().filter(BusinessRequiredOpenApiCustomizer::isSecured)
					.forEach(BusinessRequiredOpenApiCustomizer::addForbiddenResponse);
			}
		});
	}

	private boolean isGuarded(String path) {
		return pathMatcher.match(BusinessRequiredInterceptor.PROTECTED_PATTERN, path)
				&& BusinessRequiredInterceptor.EXEMPT_PATTERNS.stream().noneMatch(exempt -> pathMatcher.match(exempt, path));
	}

	// An empty @SecurityRequirements marks a public endpoint; null means the global bearerAuth applies.
	private static boolean isSecured(Operation operation) {
		return operation.getSecurity() == null || !operation.getSecurity().isEmpty();
	}

	private static void addForbiddenResponse(Operation operation) {
		if (operation.getResponses() == null || operation.getResponses().containsKey(FORBIDDEN)) {
			return;
		}
		Schema<?> problemDetail = new Schema<>().$ref("#/components/schemas/ProblemDetail");
		operation.getResponses().addApiResponse(FORBIDDEN, new ApiResponse().description(DESCRIPTION)
			.content(new Content().addMediaType("application/json", new MediaType().schema(problemDetail))));
	}

}
