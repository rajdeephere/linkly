package com.linkly.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Local stand-in for a real DNS TXT lookup (ADR-0006). Ownership is "published" in memory via
 * {@link #simulatePublish} (a dev hook that stands in for the tenant adding the record at their DNS
 * provider), so the pending → verified transition is demoable with no public DNS. The real
 * {@code DnsTxtVerifier} would resolve {@code _linkly-challenge.<hostname>} and compare the value.
 */
@Component
public class StubDnsVerifier implements DnsVerifier {

    private final Map<String, String> published = new ConcurrentHashMap<>();

    /** Dev-only: simulate the tenant having added the TXT record. */
    public void simulatePublish(String hostname, String token) {
        published.put(hostname, token);
    }

    @Override
    public boolean isPublished(String hostname, String token) {
        return token != null && token.equals(published.get(hostname));
    }
}
