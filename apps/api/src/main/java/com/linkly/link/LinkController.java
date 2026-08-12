package com.linkly.link;

import com.linkly.common.ClientIp;
import com.linkly.common.RateLimiter;
import com.linkly.config.LinklyProperties;
import com.linkly.link.dto.CreateLinkRequest;
import com.linkly.link.dto.LinkResponse;
import com.linkly.link.dto.UpdateLinkRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
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
        String ip = ClientIp.of(http);
        if (!rateLimiter.allow("rl:create:" + ip,
                props.rateLimit().createPerMinute(), Duration.ofMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "rate limit exceeded — try again shortly");
        }
        Link link = links.create(request);
        return LinkResponse.from(link, links.shortUrl(link));
    }

    @GetMapping("/{id}")
    public LinkResponse get(@PathVariable String id) {
        return links.findById(id)
                .map(link -> LinkResponse.from(link, links.shortUrl(link)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
    }

    @PatchMapping("/{id}")
    public LinkResponse update(@PathVariable String id, @Valid @RequestBody UpdateLinkRequest request) {
        Link link = links.update(id, request);
        return LinkResponse.from(link, links.shortUrl(link));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        links.delete(id);
    }
}
