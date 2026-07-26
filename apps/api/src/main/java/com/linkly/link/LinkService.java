package com.linkly.link;

import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public LinkService(LinkRepository links, KeyGenerationService kgs) {
        this.links = links;
        this.kgs = kgs;
    }

    /**
     * Create a short link for {@code destinationUrl}. The KGS hands out a code that is unique by
     * construction (ADR-0002), so no pre-check or retry loop is needed on the hot path. The
     * {@code (code)} unique index stays as a backstop; on the (should-never-happen) violation we claim
     * one more code and retry once.
     */
    @Transactional
    public Link create(String destinationUrl, String title) {
        Link link = new Link();
        link.setWorkspaceId(DEFAULT_WORKSPACE_ID);
        link.setCode(kgs.claim());
        link.setDestinationUrl(destinationUrl);
        link.setTitle(title);
        try {
            return links.saveAndFlush(link);
        } catch (DataIntegrityViolationException backstop) {
            link.setCode(kgs.claim());
            return links.saveAndFlush(link);
        }
    }

    /** Resolve a code to its link (the read hot path). */
    @Transactional(readOnly = true)
    public Optional<Link> resolve(String code) {
        return links.findByCode(code);
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
