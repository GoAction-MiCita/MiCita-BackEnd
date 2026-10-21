package com.micita.micita_project.core.auth.domain.models;

/**
 * The tokens handed to a client when a session starts or is renewed. {@code refreshToken} is the
 * raw value, never stored.
 */
public record SessionTokens(IssuedToken accessToken, String refreshToken, long refreshExpiresInSeconds) {

}
