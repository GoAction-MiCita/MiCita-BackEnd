package com.micita.micita_project.core.business.infrastructure.adapters.in.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only private resource with no role requirement. No real endpoint needs a business yet, so
 * the HU-04 rule is exercised against this one.
 */
@RestController
public class BusinessProbeController {

	public static final String PATH = "/api/test/business-probe";

	@GetMapping(PATH)
	public String probe() {
		return "ok";
	}

}
