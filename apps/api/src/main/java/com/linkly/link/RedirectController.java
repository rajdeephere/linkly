package com.linkly.link;

import com.linkly.analytics.ClickEventMessage;
import com.linkly.analytics.ClickEventPublisher;
import com.linkly.common.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
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
 * editability depend on it (ADR-0005). On a successful resolve it emits a click event
 * <b>fire-and-forget</b> (ADR-0004); the emit is async so the redirect never waits on analytics.
 *
 * <p>In Phase 2 this logic moves into a separate resolver service + edge (ADR-0001, ADR-0003); today
 * it lives in the same app.
 */
@RestController
public class RedirectController {

    private final LinkService links;
    private final ClickEventPublisher clicks;

    public RedirectController(LinkService links, ClickEventPublisher clicks) {
        this.links = links;
        this.clicks = clicks;
    }

    @GetMapping("/{code:[0-9A-Za-z]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest http) {
        ResolveOutcome outcome = links.resolve(code);

        if (outcome.status() == ResolveOutcome.Status.REDIRECT) {
            clicks.publish(new ClickEventMessage(
                    code,
                    ClientIp.of(http),
                    http.getHeader("User-Agent"),
                    http.getHeader("Referer"),
                    http.getHeader("CF-IPCountry"), // set by the edge later; null for now
                    System.currentTimeMillis()));
        }

        return switch (outcome.status()) {
            case REDIRECT -> ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(outcome.url())).build();
            case GONE -> ResponseEntity.status(HttpStatus.GONE).build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }
}
