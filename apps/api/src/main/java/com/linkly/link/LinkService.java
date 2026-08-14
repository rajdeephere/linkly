package com.linkly.link;

import com.linkly.config.LinklyProperties;
import com.linkly.domain.Domain;
import com.linkly.domain.DomainRepository;
import com.linkly.link.dto.CreateLinkRequest;
import com.linkly.link.dto.UpdateLinkRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Link management, scoped to the caller's workspace (RBAC — Day 12). */
@Service
public class LinkService {

    private final LinkRepository links;
    private final KeyGenerationService kgs;
    private final UrlSafetyChecker safety;
    private final LinkCache cache;
    private final DomainRepository domains;
    private final LinklyProperties props;

    public LinkService(LinkRepository links, KeyGenerationService kgs, UrlSafetyChecker safety,
                       LinkCache cache, DomainRepository domains, LinklyProperties props) {
        this.links = links;
        this.kgs = kgs;
        this.safety = safety;
        this.cache = cache;
        this.domains = domains;
        this.props = props;
    }

    @Transactional
    public Link create(CreateLinkRequest req, UUID workspaceId) {
        if (!safety.isSafe(req.destinationUrl())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "destination failed a safety check");
        }
        Domain domain = resolveDomain(req.domainId(), workspaceId);

        Link link = new Link();
        link.setWorkspaceId(workspaceId);
        link.setDomainId(domain.getId());
        link.setDestinationUrl(req.destinationUrl());
        link.setTitle(req.title());
        link.setExpiresAt(req.expiresAt());
        link.setExpiresUrl(req.expiresUrl());
        link.setClickLimit(req.clickLimit());

        if (req.hasAlias()) {
            if (links.existsByDomainIdAndCode(domain.getId(), req.alias())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "alias already taken");
            }
            link.setCode(req.alias());
            try {
                return links.saveAndFlush(link);
            } catch (DataIntegrityViolationException race) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "alias already taken");
            }
        }

        link.setCode(kgs.claim());
        try {
            return links.saveAndFlush(link);
        } catch (DataIntegrityViolationException backstop) {
            link.setCode(kgs.claim());
            return links.saveAndFlush(link);
        }
    }

    @Transactional(readOnly = true)
    public List<Link> list(UUID workspaceId) {
        return links.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional
    public Link update(String id, UUID workspaceId, UpdateLinkRequest req) {
        Link link = require(id, workspaceId);
        if (req.destinationUrl() != null) {
            if (!safety.isSafe(req.destinationUrl())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "destination failed a safety check");
            }
            link.setDestinationUrl(req.destinationUrl());
        }
        if (req.title() != null) {
            link.setTitle(req.title());
        }
        if (req.expiresAt() != null) {
            link.setExpiresAt(req.expiresAt());
        }
        if (req.clickLimit() != null) {
            link.setClickLimit(req.clickLimit());
        }
        if (req.expiresUrl() != null) {
            link.setExpiresUrl(req.expiresUrl());
        }
        link.setUpdatedAt(OffsetDateTime.now());
        Link saved = links.save(link);
        cache.evict(hostnameOf(saved), saved.getCode());
        return saved;
    }

    @Transactional
    public void delete(String id, UUID workspaceId) {
        Link link = require(id, workspaceId);
        links.delete(link);
        cache.evict(hostnameOf(link), link.getCode());
    }

    /** Look up a link within a workspace; empty for a malformed id or a link in another workspace. */
    @Transactional(readOnly = true)
    public Optional<Link> findById(String id, UUID workspaceId) {
        try {
            return links.findByIdAndWorkspaceId(UUID.fromString(id), workspaceId);
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public String shortUrl(Link link) {
        Domain domain = domains.findById(link.getDomainId()).orElse(null);
        String base = (domain == null || domain.isDefault())
                ? props.baseUrl()
                : "https://" + domain.getHostname();
        return base + "/" + link.getCode();
    }

    private Domain resolveDomain(String domainId, UUID workspaceId) {
        if (domainId == null || domainId.isBlank()) {
            return domains.findById(Domain.DEFAULT_ID).orElseThrow(
                    () -> new IllegalStateException("default domain missing"));
        }
        Domain domain;
        try {
            domain = domains.findById(UUID.fromString(domainId)).orElse(null);
        } catch (IllegalArgumentException e) {
            domain = null;
        }
        if (domain == null || !domain.getWorkspaceId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "domain not found");
        }
        if (!domain.isVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "domain is not verified yet");
        }
        return domain;
    }

    private Link require(String id, UUID workspaceId) {
        return findById(id, workspaceId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
    }

    private String hostnameOf(Link link) {
        return domains.findById(link.getDomainId()).map(Domain::getHostname).orElse("");
    }
}
