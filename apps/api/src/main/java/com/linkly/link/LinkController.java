package com.linkly.link;

import com.linkly.common.RateLimiter;
import com.linkly.config.LinklyProperties;
import com.linkly.link.dto.CreateLinkRequest;
import com.linkly.link.dto.LinkResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Management API for links (create / read). The redirect lives in {@link RedirectController}. */
@RestController
@RequestMapping("/v1/links")
public class LinkController {

    private final LinkService links;
    private final LinklyProperties props;
    private final RateLimiter rateLimiter;

    public LinkController(LinkService links, LinklyProperties props, RateLimiter rateLimiter) {
        this.links = links;
        this.props = props;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(@Valid @RequestBody CreateLinkRequest request, HttpServletRequest http) {
        String ip = clientIp(http);
        if (!rateLimiter.allow("rl:create:" + ip,
                props.rateLimit().createPerMinute(), Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "rate limit exceeded — try again shortly");
        }
        Link link = links.create(request);
        return LinkResponse.from(link, props.baseUrl());
    }

    @GetMapping("/{id}")
    public LinkResponse get(@PathVariable String id) {
        return links.findById(id)
                .map(link -> LinkResponse.from(link, props.baseUrl()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
    }

    /** Best-effort client IP: first X-Forwarded-For hop (behind a proxy) else the socket address. */
    private static String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
