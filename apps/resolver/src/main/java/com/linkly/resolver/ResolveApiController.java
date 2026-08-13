package com.linkly.resolver;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Origin API the edge calls:
 * <ul>
 *   <li>{@code GET /r/{code}} → JSON {@link ResolveOutcome} (status/url/cacheable) — no emission; the
 *       edge decides the HTTP response and whether to cache.</li>
 *   <li>{@code POST /ingest/click} → fire-and-forget click event — the edge posts this on every hit
 *       (cache hit or miss) so analytics is never lost when the edge serves from KV.</li>
 * </ul>
 */
@RestController
public class ResolveApiController {

    private final ResolveService resolve;
    private final ClickEventPublisher clicks;

    public ResolveApiController(ResolveService resolve, ClickEventPublisher clicks) {
        this.resolve = resolve;
        this.clicks = clicks;
    }

    @GetMapping("/r/{code:[0-9A-Za-z]+}")
    public ResolveOutcome resolve(@PathVariable String code,
                                  @RequestParam String host,
                                  @RequestParam(required = false) String ua,
                                  @RequestParam(required = false) String country,
                                  @RequestParam(required = false) String ip) {
        return resolve.resolve(code, host, RoutingContext.from(ua, country, ip));
    }

    @PostMapping("/ingest/click")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingest(@RequestBody ClickEventMessage click) {
        clicks.publish(click);
    }
}
