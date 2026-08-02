package com.linkly.resolver;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The resolve decision: cache-aside → DB, with expiry + race-free click-cap (mirrors the api, Day 5). */
@Service
public class ResolveService {

    private final LinkRepository links;
    private final LinkCache cache;

    public ResolveService(LinkRepository links, LinkCache cache) {
        this.links = links;
        this.cache = cache;
    }

    @Transactional
    public ResolveOutcome resolve(String code) {
        var cached = cache.getDestination(code);
        if (cached.isPresent()) {
            return ResolveOutcome.redirect(cached.get(), true);
        }

        Link link = links.findByCode(code).orElse(null);
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
            return ResolveOutcome.redirect(link.getDestinationUrl(), false); // time-limited → not cacheable
        }

        cache.put(code, link.getDestinationUrl());
        return ResolveOutcome.redirect(link.getDestinationUrl(), true);
    }

    private ResolveOutcome expired(Link link) {
        return link.getExpiresUrl() != null
                ? ResolveOutcome.redirect(link.getExpiresUrl(), false)
                : ResolveOutcome.gone();
    }
}
