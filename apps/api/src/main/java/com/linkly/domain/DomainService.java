package com.linkly.domain;

import com.linkly.domain.dto.CreateDomainRequest;
import com.linkly.domain.dto.DomainResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DomainService {

    private static final UUID DEFAULT_WORKSPACE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final DomainRepository domains;
    private final DnsVerifier dns;

    public DomainService(DomainRepository domains, DnsVerifier dns) {
        this.domains = domains;
        this.dns = dns;
    }

    /** Register a domain: unverified, with a fresh ownership token and pending TLS. */
    @Transactional
    public DomainResponse add(CreateDomainRequest req) {
        String hostname = req.hostname().toLowerCase();
        if (domains.existsByHostname(hostname)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "hostname already registered");
        }
        Domain d = new Domain();
        d.setWorkspaceId(DEFAULT_WORKSPACE_ID);
        d.setHostname(hostname);
        d.setVerificationToken(UUID.randomUUID().toString().replace("-", ""));
        d.setTlsStatus("pending");
        return toResponse(domains.save(d));
    }

    @Transactional(readOnly = true)
    public DomainResponse get(String id) {
        return toResponse(load(id));
    }

    /**
     * Verify ownership via the DNS TXT record. On success mark verified and provision TLS (simulated
     * here: pending → active). On failure stay pending with a clear 400.
     */
    @Transactional
    public DomainResponse verify(String id) {
        Domain d = load(id);
        if (d.isVerified()) {
            return toResponse(d);
        }
        if (!dns.isPublished(d.getHostname(), d.getVerificationToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "DNS record not found yet — add " + dns.recordName(d.getHostname())
                            + " = " + dns.recordValue(d.getVerificationToken()));
        }
        d.setVerified(true);
        d.setTlsStatus("active"); // real impl: kick off ACME / on-demand TLS issuance
        return toResponse(domains.save(d));
    }

    /** Dev-only: simulate the tenant publishing the DNS record (stands in for their DNS provider). */
    @Transactional(readOnly = true)
    public void simulateDns(String id) {
        Domain d = load(id);
        if (dns instanceof StubDnsVerifier stub) {
            stub.simulatePublish(d.getHostname(), d.getVerificationToken());
        }
    }

    private Domain load(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "domain not found");
        }
        return domains.findById(uuid).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "domain not found"));
    }

    private DomainResponse toResponse(Domain d) {
        return DomainResponse.of(d,
                dns.recordName(d.getHostname()), dns.recordValue(d.getVerificationToken()));
    }
}
