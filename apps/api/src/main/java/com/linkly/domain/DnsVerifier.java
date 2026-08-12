package com.linkly.domain;

/**
 * Checks whether a domain's ownership TXT record is published (ADR-0006). The real implementation does a
 * DNS TXT lookup at {@code _linkly-challenge.<hostname>}; {@link StubDnsVerifier} simulates it locally so
 * the verification flow is demoable without public DNS.
 */
public interface DnsVerifier {

    /** TXT record name the tenant must create for {@code hostname}. */
    default String recordName(String hostname) {
        return "_linkly-challenge." + hostname;
    }

    /** TXT record value the tenant must set (the ownership token). */
    default String recordValue(String token) {
        return "linkly-verify=" + token;
    }

    /** True when the expected TXT record for {@code hostname} carrying {@code token} is present. */
    boolean isPublished(String hostname, String token);
}
