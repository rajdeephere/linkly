package com.linkly.common;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis fixed-window rate limiter. First hit in a window sets the key + TTL; subsequent hits just
 * INCR. Simple and good enough to throttle abuse on non-hot-path endpoints (e.g. link creation).
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** @return true if the call is allowed (count within {@code limit} for the current window). */
    public boolean allow(String key, int limit, Duration window) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }
        return count != null && count <= limit;
    }
}
