package com.linkly.link;

/**
 * Screens a destination URL before a code is issued (ADR-0009). The real implementation calls Google
 * Safe Browsing; the {@link StubUrlSafetyChecker} used by default blocks a small local marker list so
 * abuse rejection is provable offline (and swaps in the real checker by config).
 */
public interface UrlSafetyChecker {
    boolean isSafe(String url);
}
