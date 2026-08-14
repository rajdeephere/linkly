package com.linkly.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT config. {@code secret} is a Base64-encoded HS256 key (≥256 bits); in prod it comes from a secrets
 * manager, never the repo.
 */
@ConfigurationProperties(prefix = "linkly.jwt")
public record JwtProperties(String secret, long expiryMinutes) {
}
