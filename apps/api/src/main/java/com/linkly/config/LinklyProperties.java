package com.linkly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * App config bound from {@code linkly.*}.
 *
 * @param baseUrl      origin used to build a link's short URL (e.g. http://localhost:8081/{code});
 *                     in production this is the branded/short domain.
 * @param rateLimit    request-throttling knobs.
 * @param safeBrowsing destination-safety screening (ADR-0009).
 */
@ConfigurationProperties(prefix = "linkly")
public record LinklyProperties(String baseUrl, RateLimit rateLimit, SafeBrowsing safeBrowsing) {

    /** @param createPerMinute max link-creations per client IP per minute. */
    public record RateLimit(int createPerMinute) {
    }

    /** @param enabled when true, use the real Safe Browsing checker (needs an API key). */
    public record SafeBrowsing(boolean enabled) {
    }
}
