package com.linkly.routing;

import com.linkly.domain.Domain;
import com.linkly.domain.DomainRepository;
import com.linkly.link.Link;
import com.linkly.link.LinkCache;
import com.linkly.link.LinkRepository;
import com.linkly.routing.dto.CreateRuleRequest;
import com.linkly.routing.dto.RuleResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoutingService {

    private final RoutingRuleRepository rules;
    private final LinkRepository links;
    private final DomainRepository domains;
    private final LinkCache cache;

    public RoutingService(RoutingRuleRepository rules, LinkRepository links,
                          DomainRepository domains, LinkCache cache) {
        this.rules = rules;
        this.links = links;
        this.domains = domains;
        this.cache = cache;
    }

    /** Add a rule to a link, then purge its cache — a link with rules is no longer plain-cacheable. */
    @Transactional
    public RuleResponse add(String linkId, UUID workspaceId, CreateRuleRequest req) {
        Link link = load(linkId, workspaceId);
        if (("AB".equals(req.type())) == (req.matchValue() != null && !req.matchValue().isBlank())) {
            // matchValue is required for DEVICE/OS/GEO and must be absent for AB.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "matchValue is required for DEVICE/OS/GEO and must be omitted for AB");
        }
        RoutingRule rule = new RoutingRule();
        rule.setLinkId(link.getId());
        rule.setType(req.type());
        rule.setMatchValue(req.matchValue());
        rule.setDestinationUrl(req.destinationUrl());
        rule.setWeight(req.weight() == null ? 1 : req.weight());
        rule.setPriority(req.priority() == null ? 100 : req.priority());
        RoutingRule saved = rules.save(rule);

        cache.evict(hostnameOf(link), link.getCode());
        return RuleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> list(String linkId, UUID workspaceId) {
        return rules.findByLinkId(load(linkId, workspaceId).getId())
                .stream().map(RuleResponse::from).toList();
    }

    private Link load(String linkId, UUID workspaceId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(linkId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found");
        }
        return links.findByIdAndWorkspaceId(uuid, workspaceId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
    }

    private String hostnameOf(Link link) {
        return domains.findById(link.getDomainId()).map(Domain::getHostname).orElse("");
    }
}
