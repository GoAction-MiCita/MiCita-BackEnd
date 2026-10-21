package com.micita.micita_project.core.auth.application.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.micita.micita_project.core.auth.application.dto.response.TokenResponse;
import com.micita.micita_project.core.auth.domain.models.IssuedToken;
import com.micita.micita_project.core.auth.domain.models.SessionTokens;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TokenMapperTest {

	private final TokenMapper tokenMapper = Mappers.getMapper(TokenMapper.class);

	@Test
	void toResponseMapsSessionTokensToBearerResponse() {
		TokenResponse response = tokenMapper
			.toResponse(new SessionTokens(new IssuedToken("jwt-token", 900), "refresh-token", 604800));

		assertThat(response).isEqualTo(new TokenResponse("jwt-token", "Bearer", 900, "refresh-token", 604800));
	}

}
