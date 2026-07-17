package com.linkly.link.dto;

import com.linkly.link.Link;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Wire representation of a link — an explicit projection, never the JPA entity. */
public record LinkResponse(
        UUID id,
        String code,
        String shortUrl,
        String destinationUrl,
        String title,
        OffsetDateTime createdAt
) {
    public static LinkResponse from(Link link, String baseUrl) {
        return new LinkResponse(
                link.getId(),
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getDestinationUrl(),
                link.getTitle(),
                link.getCreatedAt());
    }
}
