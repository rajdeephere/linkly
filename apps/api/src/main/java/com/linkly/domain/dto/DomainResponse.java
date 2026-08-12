package com.linkly.domain.dto;

import com.linkly.domain.Domain;
import java.util.UUID;

/**
 * Domain view. When unverified, carries the DNS record the tenant must add ({@code dnsRecordName} /
 * {@code dnsRecordValue}); those are null once verified.
 */
public record DomainResponse(
        UUID id,
        String hostname,
        boolean verified,
        String tlsStatus,
        String dnsRecordName,
        String dnsRecordValue
) {
    public static DomainResponse of(Domain d, String recordName, String recordValue) {
        return new DomainResponse(
                d.getId(), d.getHostname(), d.isVerified(), d.getTlsStatus(),
                d.isVerified() ? null : recordName,
                d.isVerified() ? null : recordValue);
    }
}
