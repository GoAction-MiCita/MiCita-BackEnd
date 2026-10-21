package com.micita.micita_project;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app against a throwaway PostgreSQL container: {@code ./mvnw spring-boot:test-run}.
 */
public class TestMicitaProjectApplication {

	public static void main(String[] args) {
		SpringApplication.from(MicitaProjectApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
