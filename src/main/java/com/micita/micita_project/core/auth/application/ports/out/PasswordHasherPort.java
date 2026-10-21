package com.micita.micita_project.core.auth.application.ports.out;

public interface PasswordHasherPort {

	String hash(String rawPassword);

}
