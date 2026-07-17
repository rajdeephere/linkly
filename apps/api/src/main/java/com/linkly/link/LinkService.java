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

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final LinkRepository links;
    private final CodeGenerator codeGenerator;

    public LinkService(LinkRepository links, CodeGenerator codeGenerator) {
        this.links = links;
        this.codeGenerator = codeGenerator;
    }

    /**
     * Create a short link for {@code destinationUrl}. Generates a unique code; the {@code (code)}
     * unique index is the correctness guarantee, the retry loop handles the rare collision (and the
     * even rarer concurrent-insert race, caught as a constraint violation).
     */
    @Transactional
    public Link create(String destinationUrl, String title) {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (links.existsByCode(code)) {
                continue;
            }
            Link link = new Link();
            link.setWorkspaceId(DEFAULT_WORKSPACE_ID);
            link.setCode(code);
            link.setDestinationUrl(destinationUrl);
            link.setTitle(title);
            try {
                return links.saveAndFlush(link);
            } catch (DataIntegrityViolationException race) {
                // Another request claimed this code between existsByCode and flush — try again.
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique code after " + MAX_CODE_ATTEMPTS + " attempts");
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
