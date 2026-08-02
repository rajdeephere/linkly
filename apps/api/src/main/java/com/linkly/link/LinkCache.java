package com.linkly.link;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cache-aside for the resolve hot path (ADR-0008): {@code link:{code} → destinationUrl}.
 *
 * <p>Only <b>plain</b> links (no click cap, no expiry) are cached — capped/expiring links carry
 * per-request state and must re-check the DB. A short TTL is a backstop; correctness comes from the
 * explicit {@link #evict} on edit/delete (a stale cached destination is a bug).
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

    public void evict(String code) {
        // Purge both the origin cache and the edge KV entry (ADR-0008 — the purge fans out to the edge).
        // Locally the edge KV is the same Redis via SRH; in prod this would call the KV provider's API.
        redis.delete(PREFIX + code);
        redis.delete("edge:" + PREFIX + code);
    }
}
