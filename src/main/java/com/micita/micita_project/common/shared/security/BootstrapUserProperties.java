package com.micita.micita_project.common.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ADMIN account seeded into the users table at startup if its username is not taken yet.
 */
@ConfigurationProperties("micita.security.bootstrap-user")
public record BootstrapUserProperties(String username, String password) {

}
