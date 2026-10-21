package com.micita.micita_project.core.auth.application.ports.in;

import com.micita.micita_project.core.auth.application.dto.request.RegisterUserRequest;
import com.micita.micita_project.core.auth.application.dto.response.UserResponse;

public interface RegisterUserUseCase {

	UserResponse register(RegisterUserRequest request);

}
