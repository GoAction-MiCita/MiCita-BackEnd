package com.micita.micita_project.core.business.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Data to register the caller's business during onboarding")
public record CreateBusinessRequest(
		@Schema(example = "Barbería El Corte") @NotBlank @Size(max = 150) String name,
		@Schema(description = "NIT or other registration id", example = "900123456-7", nullable = true)
		@Size(max = 30) String taxId,
		@Schema(example = "Calle 10 # 20-30, Bogotá") @NotBlank @Size(max = 255) String address,
		@Schema(example = "+573001234567") @NotBlank
		@Pattern(regexp = PHONE_PATTERN, message = "must have 7 to 15 digits, optionally starting with +") String phone,
		@Schema(example = "contacto@elcorte.com", nullable = true) @Email @Size(max = 254) String email) {

	public static final String PHONE_PATTERN = "^\\+?\\d{7,15}$";

}
