package com.micita.micita_project.core.business.application.dto.response;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A registered business")
public record BusinessResponse(
		@Schema(example = "7b3e9c1a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") UUID id,
		@Schema(example = "Barbería El Corte") String name,
		@Schema(example = "900123456-7", nullable = true) String taxId,
		@Schema(example = "Calle 10 # 20-30, Bogotá") String address,
		@Schema(example = "+573001234567") String phone,
		@Schema(example = "contacto@elcorte.com", nullable = true) String email,
		@Schema(example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) String status,
		@Schema(example = "America/Bogota") String timeZone,
		@Schema(example = "2026-10-27T15:00:00Z") Instant createdAt) {

}
