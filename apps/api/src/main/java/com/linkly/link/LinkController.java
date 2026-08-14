package com.linkly.link;

import com.linkly.auth.AppUserPrincipal;
import com.linkly.common.ClientIp;
import com.linkly.common.RateLimiter;
import com.linkly.config.LinklyProperties;
import com.linkly.link.dto.BulkResult;
import com.linkly.link.dto.CreateLinkRequest;
import com.linkly.link.dto.LinkResponse;
import com.linkly.link.dto.UpdateLinkRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Management API for links — all operations scoped to the caller's workspace. */
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
    public LinkResponse create(@Valid @RequestBody CreateLinkRequest request,
                               @AuthenticationPrincipal AppUserPrincipal me, HttpServletRequest http) {
        if (!rateLimiter.allow("rl:create:" + ClientIp.of(http),
                props.rateLimit().createPerMinute(), Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "rate limit exceeded — try again shortly");
        }
        Link link = links.create(request, me.workspaceId());
        return LinkResponse.from(link, links.shortUrl(link));
    }

    /** Bulk-create from a CSV body (destinationUrl[,alias][,title] per row). Works with an API key. */
    @PostMapping(value = "/bulk", consumes = {"text/csv", "text/plain"})
    public BulkResult bulk(@RequestBody String csv, @AuthenticationPrincipal AppUserPrincipal me) {
        return links.createBulk(csv, me.workspaceId());
    }

    @GetMapping
    public List<LinkResponse> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return links.list(me.workspaceId()).stream()
                .map(link -> LinkResponse.from(link, links.shortUrl(link))).toList();
    }

    @GetMapping("/{id}")
    public LinkResponse get(@PathVariable String id, @AuthenticationPrincipal AppUserPrincipal me) {
        return links.findById(id, me.workspaceId())
                .map(link -> LinkResponse.from(link, links.shortUrl(link)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
    }

    @PatchMapping("/{id}")
    public LinkResponse update(@PathVariable String id, @Valid @RequestBody UpdateLinkRequest request,
                               @AuthenticationPrincipal AppUserPrincipal me) {
        Link link = links.update(id, me.workspaceId(), request);
        return LinkResponse.from(link, links.shortUrl(link));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, @AuthenticationPrincipal AppUserPrincipal me) {
        if (!me.canManage()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "requires owner or admin role");
        }
        links.delete(id, me.workspaceId());
    }
}
