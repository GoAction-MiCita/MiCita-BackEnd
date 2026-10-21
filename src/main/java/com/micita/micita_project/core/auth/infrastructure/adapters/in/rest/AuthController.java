package com.micita.micita_project.core.auth.infrastructure.adapters.in.rest;

import com.micita.micita_project.core.auth.application.dto.request.LoginRequest;
import com.micita.micita_project.core.auth.application.dto.request.RefreshTokenRequest;
import com.micita.micita_project.core.auth.application.dto.request.RegisterUserRequest;
import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.application.dto.response.UserResponse;
import com.micita.micita_project.core.auth.application.ports.in.LoginUseCase;
import com.micita.micita_project.core.auth.application.ports.in.LogoutUseCase;
import com.micita.micita_project.core.auth.application.ports.in.RefreshTokenUseCase;
import com.micita.micita_project.core.auth.application.ports.in.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registration, authentication and token issuance")
public class AuthController {

	private final LoginUseCase loginUseCase;

	private final RegisterUserUseCase registerUserUseCase;

	private final RefreshTokenUseCase refreshTokenUseCase;

	private final LogoutUseCase logoutUseCase;

	public AuthController(LoginUseCase loginUseCase, RegisterUserUseCase registerUserUseCase,
			RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase) {
		this.loginUseCase = loginUseCase;
		this.registerUserUseCase = registerUserUseCase;
		this.refreshTokenUseCase = refreshTokenUseCase;
		this.logoutUseCase = logoutUseCase;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@SecurityRequirements
	@Operation(summary = "Register", description = "Creates a business-owner account with no roles")
	@ApiResponse(responseCode = "201", description = "User registered")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields, or a weak password",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Email or username already registered",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
		return registerUserUseCase.register(request);
	}

	@PostMapping("/login")
	@SecurityRequirements
	@Operation(summary = "Log in",
			description = "Exchanges credentials for a short-lived JWT access token (with the roles and hasBusiness "
					+ "claims) and a refresh token")
	@ApiResponse(responseCode = "200", description = "Tokens issued")
	@ApiResponse(responseCode = "400", description = "Invalid request body",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Invalid credentials",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return loginUseCase.login(request);
	}

	@PostMapping("/refresh")
	@SecurityRequirements
	@Operation(summary = "Refresh tokens",
			description = "Exchanges a valid refresh token for a new access token and a new refresh token. The "
					+ "refresh token sent is revoked, so each one can be used once")
	@ApiResponse(responseCode = "200", description = "Tokens issued")
	@ApiResponse(responseCode = "400", description = "Invalid request body",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "401", description = "Refresh token unknown, expired or revoked: log in again",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return refreshTokenUseCase.refresh(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirements
	@Operation(summary = "Log out",
			description = "Revokes the refresh token so it can no longer be used. Unknown or already revoked tokens "
					+ "are accepted too")
	@ApiResponse(responseCode = "204", description = "Refresh token revoked")
	@ApiResponse(responseCode = "400", description = "Invalid request body",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	public void logout(@Valid @RequestBody RefreshTokenRequest request) {
		logoutUseCase.logout(request);
	}

}
