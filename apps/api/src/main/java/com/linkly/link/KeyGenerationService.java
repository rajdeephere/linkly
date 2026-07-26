package com.linkly.link;

import java.math.BigInteger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Key Generation Service (ADR-0002).
 *
 * <p>Hands out short codes that are <b>unique forever</b> and <b>non-enumerable</b>, with an O(1) claim
 * on the hot path and no collision check on the write path.
 *
 * <p><b>How.</b> A monotonic Redis counter ({@code INCR kgs:counter}) drives generation. Each counter
 * value {@code n} is mapped through an <b>affine permutation</b> over the code space
 * {@code x -> (A*n + B) mod 62^7}. Because {@code gcd(A, 62^7) == 1} this is a <b>bijection</b>: distinct
 * counters always yield distinct codes (so no code is ever regenerated — uniqueness needs no lookup),
 * while multiplying by a large {@code A} scatters consecutive counters across the space (so codes are
 * not sequential and can't be walked). Codes are pre-generated into a Redis <b>pool</b> and claimed with
 * an atomic {@code SPOP}; a low-watermark refill keeps it stocked.
 *
 * <p><b>Note.</b> The affine scramble defeats naive sequential enumeration (the real threat); it is
 * obfuscation-grade, not cryptographic. A keyed format-preserving cipher (Feistel/FPE) is the hardening
 * step if outputs must be unpredictable even to someone collecting many codes.
 */
@Service
public class KeyGenerationService {

    static final int CODE_LENGTH = 7;
    /** 62^7 ≈ 3.52e12 codes. */
    private static final BigInteger DOMAIN = BigInteger.valueOf(62).pow(CODE_LENGTH);
    /** Odd and not a multiple of 31 → coprime to 62^7 = 2^7·31^7 → the affine map is a bijection. */
    private static final BigInteger A = BigInteger.valueOf(2_654_435_761L);
    private static final BigInteger B = BigInteger.valueOf(1_013_904_223L);

    private static final String COUNTER_KEY = "kgs:counter";
    private static final String POOL_KEY = "kgs:pool";
    private static final int LOW_WATERMARK = 200;
    private static final int REFILL_BATCH = 1000;

    private final StringRedisTemplate redis;

    public KeyGenerationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Warm the pool at startup (best-effort — the first claim would refill anyway). */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try {
            maybeRefill();
        } catch (RuntimeException redisNotReady) {
            // Non-fatal: the pool fills on first claim once Redis is reachable.
        }
    }

    /** Claim a unique, non-sequential code. O(1) on the hot path (atomic SPOP). */
    public String claim() {
        String code = redis.opsForSet().pop(POOL_KEY);
        if (code == null) {
            refill(REFILL_BATCH);
            code = redis.opsForSet().pop(POOL_KEY);
        }
        maybeRefill();
        return code != null ? code : nextCode();
    }

    /** Top up the pool if it has dropped below the low watermark. */
    public void maybeRefill() {
        Long size = redis.opsForSet().size(POOL_KEY);
        if (size == null || size < LOW_WATERMARK) {
            refill(REFILL_BATCH);
        }
    }

    /**
     * Generate {@code count} fresh codes into the pool. Every counter value maps bijectively to a
     * distinct code, so entries are unique by construction — SADD never needs to dedup.
     */
    public void refill(int count) {
        String[] codes = new String[count];
        for (int i = 0; i < count; i++) {
            codes[i] = nextCode();
        }
        redis.opsForSet().add(POOL_KEY, codes);
    }

    private String nextCode() {
        Long n = redis.opsForValue().increment(COUNTER_KEY); // atomic, monotonic
        BigInteger scrambled = A.multiply(BigInteger.valueOf(n)).add(B).mod(DOMAIN);
        return Base62.encode(scrambled, CODE_LENGTH);
    }

    public long poolSize() {
        Long size = redis.opsForSet().size(POOL_KEY);
        return size == null ? 0 : size;
    }
}
