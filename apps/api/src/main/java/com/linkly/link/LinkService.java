package com.linkly.link;

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

    public LinkService(LinkRepository links, KeyGenerationService kgs, UrlSafetyChecker safety,
                       LinkCache cache) {
        this.links = links;
        this.kgs = kgs;
        this.safety = safety;
        this.cache = cache;
    }

    /**
     * Create a short link. Screens the destination for safety (ADR-0009), then either honours a custom
     * alias (409 if taken) or claims a KGS code (unique by construction, ADR-0002). The {@code (code)}
     * unique index backstops both against a concurrent-insert race.
     */
    @Transactional
    public Link create(CreateLinkRequest req) {
        if (!safety.isSafe(req.destinationUrl())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "destination failed a safety check");
        }

        Link link = new Link();
        link.setWorkspaceId(DEFAULT_WORKSPACE_ID);
        link.setDestinationUrl(req.destinationUrl());
        link.setTitle(req.title());
        link.setExpiresAt(req.expiresAt());
        link.setExpiresUrl(req.expiresUrl());
        link.setClickLimit(req.clickLimit());

        if (req.hasAlias()) {
            if (links.existsByCode(req.alias())) {
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

    /**
     * Resolve a code: 302 to the destination, 410 (or fallback redirect) if expired / click-capped,
     * or not-found. Plain links (no cap, no expiry) are served cache-aside from Redis; capped/expiring
     * links always hit the DB because they carry per-request state (ADR-0008).
     */
    @Transactional
    public ResolveOutcome resolve(String code) {
        Optional<String> cached = cache.getDestination(code);
        if (cached.isPresent()) {
            return ResolveOutcome.redirect(cached.get());
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
                return expired(link); // click cap reached
            }
            return ResolveOutcome.redirect(link.getDestinationUrl()); // capped → never cached
        }
        if (link.getExpiresAt() != null) {
            return ResolveOutcome.redirect(link.getDestinationUrl()); // time-limited → re-check each time
        }

        // Plain link — safe to cache.
        cache.put(code, link.getDestinationUrl());
        return ResolveOutcome.redirect(link.getDestinationUrl());
    }

    private ResolveOutcome expired(Link link) {
        return link.getExpiresUrl() != null
                ? ResolveOutcome.redirect(link.getExpiresUrl())
                : ResolveOutcome.gone();
    }

    /** Edit a link, then purge its cache entry so the next resolve reflects the change immediately. */
    @Transactional
    public Link update(String id, UpdateLinkRequest req) {
        Link link = findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
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
        cache.evict(saved.getCode());
        return saved;
    }

    /** Delete a link, then purge its cache entry (next resolve → 404). */
    @Transactional
    public void delete(String id) {
        Link link = findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
        links.delete(link);
        cache.evict(link.getCode());
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
}
