package com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.mappers;

import com.micita.micita_project.core.auth.domain.models.User;
import com.micita.micita_project.core.auth.infrastructure.adapters.out.persistence.jpa.entities.UserEntity;
import org.mapstruct.Mapper;

@Mapper
public interface UserBDMapper {

	UserEntity toEntity(User user);

	User toDomain(UserEntity entity);

}
