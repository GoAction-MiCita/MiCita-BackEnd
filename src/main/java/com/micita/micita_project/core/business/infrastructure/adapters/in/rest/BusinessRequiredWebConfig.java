package com.micita.micita_project.core.business.infrastructure.adapters.in.rest;

import com.micita.micita_project.core.business.application.ports.in.BusinessOwnershipQueryUseCase;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class BusinessRequiredWebConfig implements WebMvcConfigurer {

	private final BusinessOwnershipQueryUseCase businessOwnershipQuery;

	public BusinessRequiredWebConfig(BusinessOwnershipQueryUseCase businessOwnershipQuery) {
		this.businessOwnershipQuery = businessOwnershipQuery;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new BusinessRequiredInterceptor(businessOwnershipQuery))
			.addPathPatterns(BusinessRequiredInterceptor.PROTECTED_PATTERN)
			.excludePathPatterns(BusinessRequiredInterceptor.EXEMPT_PATTERNS);
	}

}
