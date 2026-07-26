package com.linkly.link;

import java.math.BigInteger;

/** Fixed-width base62 encoding for short codes. */
public final class Base62 {

    static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final BigInteger BASE = BigInteger.valueOf(62);

    private Base62() {
    }

    /**
     * Encode {@code value} to exactly {@code width} base62 chars (left-padded with the zero-digit).
     * Assumes {@code 0 <= value < 62^width}.
     */
    public static String encode(BigInteger value, int width) {
        char[] out = new char[width];
        BigInteger v = value;
        for (int i = width - 1; i >= 0; i--) {
            BigInteger[] qr = v.divideAndRemainder(BASE);
            out[i] = ALPHABET.charAt(qr[1].intValue());
            v = qr[0];
        }
        return new String(out);
    }
}
