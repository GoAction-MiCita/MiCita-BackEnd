package com.micita.micita_project.core.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Data for a new business-owner account")
public record RegisterUserRequest(
		@Schema(example = "Ana") @NotBlank @Size(max = 100) String firstName,
		@Schema(example = "Pérez") @NotBlank @Size(max = 100) String lastName,
		@Schema(example = "Calle 10 # 20-30, Bogotá") @NotBlank @Size(max = 255) String address,
		@Schema(example = "+573001234567") @NotBlank
		@Pattern(regexp = PHONE_PATTERN, message = "must have 7 to 15 digits, optionally starting with +") String phone,
		@Schema(example = "ana@example.com") @NotBlank @Email @Size(max = 254) String email,
		@Schema(example = "ana.perez") @NotBlank @Size(max = 50) String username,
		@Schema(example = "Secret#1", minLength = 6, maxLength = 72) @NotBlank
		@Size(max = 72, message = "must have at most 72 characters")
		@Pattern(regexp = PASSWORD_PATTERN,
				message = "must have at least 6 characters, including lowercase, uppercase and special characters") String password) {

	public static final String PHONE_PATTERN = "^\\+?\\d{7,15}$";

	/** At least 6 characters with a lowercase letter, an uppercase letter and a special character. */
	public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9\\s]).{6,}$";

}
