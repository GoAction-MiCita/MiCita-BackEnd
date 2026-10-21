package com.micita.micita_project.core.auth.application.mappers;

import com.micita.micita_project.core.auth.application.dto.request.RegisterUserRequest;
import com.micita.micita_project.core.auth.application.dto.response.UserResponse;
import com.micita.micita_project.core.auth.domain.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

	/**
	 * Builds an unsaved user with no roles. The raw password is never copied.
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "email", source = "normalizedEmail")
	@Mapping(target = "passwordHash", source = "passwordHash")
	@Mapping(target = "roles", expression = "java(java.util.Set.of())")
	User toNewUser(RegisterUserRequest request, String normalizedEmail, String passwordHash);

	UserResponse toResponse(User user);

}
