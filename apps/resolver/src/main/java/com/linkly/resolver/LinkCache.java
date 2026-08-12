package com.linkly.resolver;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Origin-side cache-aside, host-scoped ({@code link:{host}:{code}}) since uniqueness is per
 * (domain, code). The api purges these keys on edit; the edge KV is a second layer in front.
 */
@Component
public class LinkCache {

    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    public LinkCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static String key(String host, String code) {
        return "link:" + host + ":" + code;
    }

    public Optional<String> getDestination(String host, String code) {
        return Optional.ofNullable(redis.opsForValue().get(key(host, code)));
    }

    public void put(String host, String code, String destinationUrl) {
        redis.opsForValue().set(key(host, code), destinationUrl, TTL);
    }
}
