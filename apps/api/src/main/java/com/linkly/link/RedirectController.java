package com.linkly.link;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The redirect hot path: {@code GET /{code}} → 302 to the destination.
 *
 * <p>Returns <b>302</b> (not 301) by default so every click keeps flowing through us — analytics and
 * editability depend on it (ADR-0005). The path variable is constrained to base62 characters so
 * requests like {@code /favicon.ico} or {@code /actuator/...} never match here; the literal
 * {@code /ping} route also out-ranks this pattern in Spring's path matching.
 *
 * <p>In Phase 2 this logic moves into a separate resolver service + edge (ADR-0001, ADR-0003); today
 * it lives in the same app.
 */
@RestController
public class RedirectController {

    private final LinkService links;

    public RedirectController(LinkService links) {
        this.links = links;
    }

    @GetMapping("/{code:[0-9A-Za-z]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return links.resolve(code)
                .map(link -> ResponseEntity
                        .status(HttpStatus.FOUND)
                        .location(URI.create(link.getDestinationUrl()))
                        .<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
