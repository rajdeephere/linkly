package com.linkly.link;

import com.linkly.config.LinklyProperties;
import com.linkly.link.dto.CreateLinkRequest;
import com.linkly.link.dto.LinkResponse;
import jakarta.validation.Valid;
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

    public LinkController(LinkService links, LinklyProperties props) {
        this.links = links;
        this.props = props;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(@Valid @RequestBody CreateLinkRequest request) {
        Link link = links.create(request.destinationUrl(), request.title());
        return LinkResponse.from(link, props.baseUrl());
    }

    @GetMapping("/{id}")
    public LinkResponse get(@PathVariable String id) {
        return links.findById(id)
                .map(link -> LinkResponse.from(link, props.baseUrl()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
    }
}
