package com.micita.micita_project.core.business.infrastructure.adapters.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.micita.micita_project.common.shared.security.SecurityConfig;
import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.business.application.ports.in.CreateBusinessUseCase;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * HU-04: without a business, protected endpoints answer 403 BUSINESS_REQUIRED while onboarding stays
 * reachable; once the business exists they open up.
 */
@WebMvcTest({ BusinessProbeController.class, BusinessController.class })
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class BusinessRequiredAccessTest {

	private static final UUID USER_ID = UUID.fromString("3f1c2a7e-5b8d-4c1e-9a2f-6d7e8f9a0b1c");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BusinessOwnershipQueryUseCase businessOwnershipQuery;

	@MockitoBean
	private CreateBusinessUseCase createBusinessUseCase;

	// SecurityConfig's AuthenticationManager needs one; the JPA implementation is outside the slice.
	@MockitoBean
	private UserDetailsService userDetailsService;

	@Test
	void userWithoutBusinessIsForbiddenWithBusinessRequiredCode() throws Exception {
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);

		mockMvc.perform(get(BusinessProbeController.PATH).with(userJwt()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.code").value("BUSINESS_REQUIRED"));
	}

	@Test
	void userWithBusinessIsAllowed() throws Exception {
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(true);

		mockMvc.perform(get(BusinessProbeController.PATH).with(userJwt()))
			.andExpect(status().isOk())
			.andExpect(content().string("ok"));
	}

	@Test
	void onboardingIsReachableWithoutBusiness() throws Exception {
		mockMvc.perform(post(BusinessController.ONBOARDING_PATH).with(userJwt())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"El Corte\",\"address\":\"Calle 10\",\"phone\":\"+573001234567\"}"))
			.andExpect(status().isCreated());
		verifyNoInteractions(businessOwnershipQuery);
	}

	@Test
	void unauthenticatedRequestIsUnauthorizedBeforeTheBusinessCheck() throws Exception {
		mockMvc.perform(get(BusinessProbeController.PATH)).andExpect(status().isUnauthorized());
		verifyNoInteractions(businessOwnershipQuery);
	}

	@Test
	void tokenWithoutUserIdIsUnauthorized() throws Exception {
		mockMvc.perform(get(BusinessProbeController.PATH).with(jwt())).andExpect(status().isUnauthorized());
		verifyNoInteractions(businessOwnershipQuery);
	}

	@Test
	void businessIsCheckedOnEveryRequestSoAccessOpensRightAfterOnboarding() throws Exception {
		given(businessOwnershipQuery.ownsBusiness(eq(USER_ID))).willReturn(false, true);

		mockMvc.perform(get(BusinessProbeController.PATH).with(userJwt())).andExpect(status().isForbidden());
		mockMvc.perform(get(BusinessProbeController.PATH).with(userJwt())).andExpect(status().isOk());
	}

	@Test
	void ownershipIsLookedUpForTheTokenUser() throws Exception {
		given(businessOwnershipQuery.ownsBusiness(any())).willReturn(true);
		given(businessOwnershipQuery.ownsBusiness(USER_ID)).willReturn(false);

		mockMvc.perform(get(BusinessProbeController.PATH).with(userJwt())).andExpect(status().isForbidden());
	}

	private static RequestPostProcessor userJwt() {
		return jwt().jwt(token -> token.claim(SecurityConfig.USER_ID_CLAIM, USER_ID.toString()));
	}

}
