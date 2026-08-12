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
        OffsetDateTime expiresAt,
        Long clickLimit,
        OffsetDateTime createdAt
) {
    public static LinkResponse from(Link link, String shortUrl) {
        return new LinkResponse(
                link.getId(),
                link.getCode(),
                shortUrl,
                link.getDestinationUrl(),
                link.getTitle(),
                link.getExpiresAt(),
                link.getClickLimit(),
                link.getCreatedAt());
    }
}
