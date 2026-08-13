package com.linkly.resolver;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The resolve decision (Day 10 Host-scoped, Day 11 rule-aware). Cache-aside → DB, with expiry,
 * race-free click-cap, and resolve-time routing rules. A link with rules (or a cap/expiry) is never
 * cached, since its destination is per-request.
 */
@Service
public class ResolveService {

    private final LinkRepository links;
    private final DomainRepository domains;
    private final RoutingRuleRepository ruleRepo;
    private final RoutingEvaluator evaluator;
    private final LinkCache cache;

    public ResolveService(LinkRepository links, DomainRepository domains,
                          RoutingRuleRepository ruleRepo, RoutingEvaluator evaluator, LinkCache cache) {
        this.links = links;
        this.domains = domains;
        this.ruleRepo = ruleRepo;
        this.evaluator = evaluator;
        this.cache = cache;
    }

    @Transactional
    public ResolveOutcome resolve(String code, String host, RoutingContext ctx) {
        Domain domain = domains.findByHostname(host).orElse(null);
        if (domain == null) {
            return ResolveOutcome.notFound();
        }

        var cached = cache.getDestination(host, code);
        if (cached.isPresent()) {
            return ResolveOutcome.redirect(cached.get(), true); // only plain links are ever cached
        }

        Link link = links.findByDomainIdAndCode(domain.getId(), code).orElse(null);
        if (link == null) {
            return ResolveOutcome.notFound();
        }
        if (link.getExpiresAt() != null && OffsetDateTime.now().isAfter(link.getExpiresAt())) {
            return expired(link);
        }
        if (link.getClickLimit() != null && links.tryIncrementClick(link.getId()) == 0) {
            return expired(link); // click cap reached
        }

        List<RoutingRule> rules = ruleRepo.findByLinkId(link.getId());
        String destination = rules.isEmpty()
                ? link.getDestinationUrl()
                : evaluator.pick(rules, ctx, link.getDestinationUrl());

        boolean cacheable = rules.isEmpty()
                && link.getClickLimit() == null
                && link.getExpiresAt() == null;
        if (cacheable) {
            cache.put(host, code, destination);
        }
        return ResolveOutcome.redirect(destination, cacheable);
    }

    private ResolveOutcome expired(Link link) {
        return link.getExpiresUrl() != null
                ? ResolveOutcome.redirect(link.getExpiresUrl(), false)
                : ResolveOutcome.gone();
    }
}
