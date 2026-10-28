package com.micita.micita_project.common.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.micita.micita_project.common.shared.exception.ConflictException;
import com.micita.micita_project.common.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void resourceNotFoundMapsTo404WithMessage() {
		ProblemDetail problem = handler.handleResourceNotFound(new ResourceNotFoundException("Servicio 7 no existe"));

		assertThat(problem.getStatus()).isEqualTo(404);
		assertThat(problem.getDetail()).isEqualTo("Servicio 7 no existe");
	}

	@Test
	void conflictMapsTo409WithMessage() {
		ProblemDetail problem = handler.handleConflict(new ConflictException("Horario no disponible"));

		assertThat(problem.getStatus()).isEqualTo(409);
		assertThat(problem.getDetail()).isEqualTo("Horario no disponible");
	}

	@Test
	void forbiddenMapsTo403WithCode() {
		ProblemDetail problem = handler.handleForbidden(new ForbiddenException("BUSINESS_REQUIRED", "Register first"));

		assertThat(problem.getStatus()).isEqualTo(403);
		assertThat(problem.getDetail()).isEqualTo("Register first");
		assertThat(problem.getProperties()).containsEntry("code", "BUSINESS_REQUIRED");
	}

	@Test
	void accessDeniedMapsTo403() {
		ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("nope"));

		assertThat(problem.getStatus()).isEqualTo(403);
	}

	@Test
	void unexpectedExceptionMapsTo500WithoutLeakingMessage() {
		ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("db password is hunter2"));

		assertThat(problem.getStatus()).isEqualTo(500);
		assertThat(problem.getDetail()).isEqualTo("Unexpected error");
	}

}
