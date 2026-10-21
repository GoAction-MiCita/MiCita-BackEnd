package com.micita.micita_project.common.shared.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only private resource restricted to ADMIN. No real endpoint requires a role yet, so the
 * 401/403 behaviour of {@link SecurityConfig} is exercised against this one.
 */
@RestController
public class AdminProbeController {

	public static final String PATH = "/api/test/admin-probe";

	@GetMapping(PATH)
	@PreAuthorize("hasRole('ADMIN')")
	public String probe() {
		return "ok";
	}

}
