package com.linkly.link;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates short codes.
 *
 * <p><b>Day 2:</b> a simple random base62 generator — uniqueness is guaranteed by the
 * {@code (code)} unique index plus a retry loop in {@link LinkService}. Random (not sequential) codes
 * are already non-enumerable; <b>Day 3</b> replaces this with a proper Key Generation Service
 * (pre-allocated pool, no write-path retry) — see ADR-0002.
 */
@Component
public class CodeGenerator {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int DEFAULT_LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
