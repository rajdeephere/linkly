package com.linkly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * App config bound from {@code linkly.*}.
 *
 * @param baseUrl origin used to build a link's short URL (e.g. http://localhost:8081/{code}).
 *                In production this is the branded/short domain.
 */
@ConfigurationProperties(prefix = "linkly")
public record LinklyProperties(String baseUrl) {
}
