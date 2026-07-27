package com.linkly.link;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Offline-provable stand-in for Google Safe Browsing (ADR-0009): flags any destination containing a
 * known-bad marker. This lets the abuse-rejection path be demonstrated without an API key; the real
 * {@code GoogleSafeBrowsingChecker} is a config-flag drop-in for the same interface.
 */
@Component
public class StubUrlSafetyChecker implements UrlSafetyChecker {

    private static final List<String> BAD_MARKERS = List.of(
            "malware",
            "phishing",
            "testsafebrowsing.appspot.com" // Google's official Safe Browsing test host
    );

    @Override
    public boolean isSafe(String url) {
        String lower = url.toLowerCase();
        return BAD_MARKERS.stream().noneMatch(lower::contains);
    }
}
