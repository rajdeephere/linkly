package com.linkly.resolver;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The resolve decision, scoped by Host → domain → link (Day 10). Cache-aside → DB, with expiry +
 * race-free click-cap. An unknown host resolves to nothing (can't serve on an unregistered domain).
 */
@Service
public class ResolveService {

    private final LinkRepository links;
    private final DomainRepository domains;
    private final LinkCache cache;

    public ResolveService(LinkRepository links, DomainRepository domains, LinkCache cache) {
        this.links = links;
        this.domains = domains;
        this.cache = cache;
    }

    @Transactional
    public ResolveOutcome resolve(String code, String host) {
        Domain domain = domains.findByHostname(host).orElse(null);
        if (domain == null) {
            return ResolveOutcome.notFound(); // request on an unregistered host
        }

        var cached = cache.getDestination(host, code);
        if (cached.isPresent()) {
            return ResolveOutcome.redirect(cached.get(), true);
        }

        Link link = links.findByDomainIdAndCode(domain.getId(), code).orElse(null);
        if (link == null) {
            return ResolveOutcome.notFound();
        }
        if (link.getExpiresAt() != null && OffsetDateTime.now().isAfter(link.getExpiresAt())) {
            return expired(link);
        }
        if (link.getClickLimit() != null) {
            if (links.tryIncrementClick(link.getId()) == 0) {
                return expired(link);
            }
            return ResolveOutcome.redirect(link.getDestinationUrl(), false); // capped → not cacheable
        }
        if (link.getExpiresAt() != null) {
            return ResolveOutcome.redirect(link.getDestinationUrl(), false); // time-limited
        }

        cache.put(host, code, link.getDestinationUrl());
        return ResolveOutcome.redirect(link.getDestinationUrl(), true);
    }

    private ResolveOutcome expired(Link link) {
        return link.getExpiresUrl() != null
                ? ResolveOutcome.redirect(link.getExpiresUrl(), false)
                : ResolveOutcome.gone();
    }
}
