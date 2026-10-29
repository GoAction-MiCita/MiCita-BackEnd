package com.micita.micita_project.core.business.infrastructure.adapters.in.rest;

import java.util.List;

import com.micita.micita_project.common.shared.security.CurrentUserId;
import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;
import com.micita.micita_project.core.business.domain.exceptions.BusinessRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HU-04: an authenticated user without a business gets 403 {@code BUSINESS_REQUIRED} on every API
 * endpoint except the {@link #EXEMPT_PATTERNS exempt ones}. New endpoints are covered by default.
 * Ownership is read from PostgreSQL on each request, so access opens as soon as the business is
 * created, even with an access token that still says {@code hasBusiness=false}. Anonymous requests
 * pass through: Spring Security already decided they may reach a public endpoint.
 */
@RequiredArgsConstructor
public class BusinessRequiredInterceptor implements HandlerInterceptor {

	public static final String PROTECTED_PATTERN = "/api/**";

	/** Session management and onboarding itself must work before the business exists. */
	public static final List<String> EXEMPT_PATTERNS = List.of("/api/auth/**", BusinessController.ONBOARDING_PATH);

	private final BusinessOwnershipQueryUseCase businessOwnershipQuery;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token
				&& !businessOwnershipQuery.ownsBusiness(CurrentUserId.from(token.getToken()))) {
			throw new BusinessRequiredException();
		}
		return true;
	}

}
