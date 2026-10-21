package com.micita.micita_project.core.auth.domain.models;

public record IssuedToken(String value, long expiresInSeconds) {

}
