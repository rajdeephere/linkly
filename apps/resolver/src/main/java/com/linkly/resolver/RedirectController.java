package com.linkly.resolver;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standalone public redirect: {@code GET /{code}} → 302 (+ fire-and-forget click event). Used when the
 * resolver serves traffic directly; the edge instead calls {@link ResolveApiController} for JSON + does
 * its own emission via {@code /ingest/click}.
 */
@RestController
public class RedirectController {

    private final ResolveService resolve;
    private final ClickEventPublisher clicks;

    public RedirectController(ResolveService resolve, ClickEventPublisher clicks) {
        this.resolve = resolve;
        this.clicks = clicks;
    }

    @GetMapping("/{code:[0-9A-Za-z]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest http) {
        ResolveOutcome outcome = resolve.resolve(code, http.getHeader("Host"));

        if (outcome.status() == ResolveOutcome.Status.REDIRECT) {
            clicks.publish(new ClickEventMessage(code, ClientIp.of(http),
                    http.getHeader("User-Agent"), http.getHeader("Referer"),
                    http.getHeader("CF-IPCountry"), System.currentTimeMillis()));
        }

        return switch (outcome.status()) {
            case REDIRECT -> ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(outcome.url())).build();
            case GONE -> ResponseEntity.status(HttpStatus.GONE).build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }
}
