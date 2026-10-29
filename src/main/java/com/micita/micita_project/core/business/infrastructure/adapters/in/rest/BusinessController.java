package com.micita.micita_project.core.business.infrastructure.adapters.in.rest;

import com.micita.micita_project.common.shared.security.CurrentUserId;
import com.micita.micita_project.core.business.application.dto.request.CreateBusinessRequest;
import com.micita.micita_project.core.business.application.dto.response.BusinessResponse;
import com.micita.micita_project.core.business.application.ports.in.CreateBusinessUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business")
@Tag(name = "Businesses", description = "Business registration and configuration")
@RequiredArgsConstructor
public class BusinessController {

	public static final String ONBOARDING_PATH = "/api/business/onboarding";

	private final CreateBusinessUseCase createBusinessUseCase;

	@PostMapping("/onboarding")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register my business",
			description = "Creates the caller's business (active, time zone America/Bogota) and links it to the "
					+ "authenticated user. The access token keeps hasBusiness=false until the client calls "
					+ "POST /api/auth/refresh")
	@ApiResponse(responseCode = "201", description = "Business registered")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token")
	@ApiResponse(responseCode = "409", description = "The user already has a business",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public BusinessResponse onboard(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateBusinessRequest request) {
		return createBusinessUseCase.create(CurrentUserId.from(jwt), request);
	}

}
