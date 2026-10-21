package com.micita.micita_project.core.auth.application.mappers;

import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.domain.models.SessionTokens;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TokenMapper {

	@Mapping(target = "accessToken", source = "accessToken.value")
	@Mapping(target = "tokenType", constant = "Bearer")
	@Mapping(target = "expiresIn", source = "accessToken.expiresInSeconds")
	@Mapping(target = "refreshExpiresIn", source = "refreshExpiresInSeconds")
	TokenResponse toResponse(SessionTokens tokens);

}
