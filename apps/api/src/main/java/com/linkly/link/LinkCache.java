package com.linkly.link;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cache invalidation for the resolve hot path (ADR-0008). The resolver + edge own the *read* caches;
 * the management API only ever *purges* them on edit/delete. Keys are host-scoped ({@code {host}:{code}})
 * because uniqueness is per (domain, code) — two hosts can share a code.
 */
@Component
public class LinkCache {

    private final StringRedisTemplate redis;

    public LinkCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Purge both the origin cache and the edge KV entry for this host+code (purge fan-out). */
    public void evict(String hostname, String code) {
        redis.delete("link:" + hostname + ":" + code);
        redis.delete("edge:link:" + hostname + ":" + code);
    }
}
