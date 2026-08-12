package com.linkly.link;

import com.linkly.config.LinklyProperties;
import com.linkly.domain.Domain;
import com.linkly.domain.DomainRepository;
import com.linkly.link.dto.CreateLinkRequest;
import com.linkly.link.dto.UpdateLinkRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LinkService {

    /**
     * The seeded demo workspace (V1 migration). Real workspace resolution arrives with auth/teams
     * (Phase 4); until then every link belongs here.
     */
    private static final UUID DEFAULT_WORKSPACE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

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

    /**
     * Create a short link. Screens the destination (ADR-0009), resolves the target domain (custom or
     * the default), then honours a custom alias (409 if taken on that domain) or claims a KGS code.
     * Uniqueness is per {@code (domain, code)} — the unique index backstops the race.
     */
    @Transactional
    public Link create(CreateLinkRequest req) {
        if (!safety.isSafe(req.destinationUrl())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "destination failed a safety check");
        }
        Domain domain = resolveDomain(req.domainId());

        Link link = new Link();
        link.setWorkspaceId(DEFAULT_WORKSPACE_ID);
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

    /** Edit a link, then purge its (host-scoped) cache entry so the next resolve reflects it. */
    @Transactional
    public Link update(String id, UpdateLinkRequest req) {
        Link link = requireById(id);
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

    /** Delete a link, then purge its cache entry (next resolve → 404). */
    @Transactional
    public void delete(String id) {
        Link link = requireById(id);
        links.delete(link);
        cache.evict(hostnameOf(link), link.getCode());
    }

    /** Look up a link by its id; empty (not an exception) for a malformed id. */
    @Transactional(readOnly = true)
    public Optional<Link> findById(String id) {
        try {
            return links.findById(UUID.fromString(id));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    /** Build a link's public short URL from its domain (default host uses the configured base URL). */
    @Transactional(readOnly = true)
    public String shortUrl(Link link) {
        Domain domain = domains.findById(link.getDomainId()).orElse(null);
        String base = (domain == null || domain.isDefault())
                ? props.baseUrl()
                : "https://" + domain.getHostname();
        return base + "/" + link.getCode();
    }

    private Domain resolveDomain(String domainId) {
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
        if (domain == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "domain not found");
        }
        if (!domain.isVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "domain is not verified yet");
        }
        return domain;
    }

    private Link requireById(String id) {
        return findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
    }

    private String hostnameOf(Link link) {
        return domains.findById(link.getDomainId()).map(Domain::getHostname).orElse("");
    }
}
