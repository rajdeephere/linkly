package com.linkly.resolver;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Origin-side cache-aside ({@code link:{code} → destinationUrl}). Shares the keyspace with the api's
 * LinkCache, so an api edit that purges {@code link:{code}} also clears the resolver's view. Only plain
 * links (no cap/expiry) are cached; the edge KV is a second layer in front of this (ADR-0003/0008).
 */
@Component
public class LinkCache {

    private static final String PREFIX = "link:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    public LinkCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<String> getDestination(String code) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIX + code));
    }

    public void put(String code, String destinationUrl) {
        redis.opsForValue().set(PREFIX + code, destinationUrl, TTL);
    }
}
