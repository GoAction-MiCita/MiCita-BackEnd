package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.mappers;

import com.micita.micita_project.core.auth.domain.models.RefreshToken;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.entities.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper
public interface RefreshTokenBDMapper {

	RefreshTokenEntity toEntity(RefreshToken refreshToken);

	RefreshToken toDomain(RefreshTokenEntity entity);

}
