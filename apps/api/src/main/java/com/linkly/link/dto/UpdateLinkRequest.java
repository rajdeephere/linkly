package com.linkly.link.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Partial update for a link — every field is optional; a {@code null} means "leave unchanged".
 * (Code/alias is immutable once issued.) Validation only fires on fields that are present.
 */
public record UpdateLinkRequest(
        @Pattern(regexp = "^https?://.+",
                message = "destinationUrl must start with http:// or https://")
        String destinationUrl,

        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @Future(message = "expiresAt must be in the future")
        OffsetDateTime expiresAt,

        @Positive(message = "clickLimit must be positive")
        Long clickLimit,

        @Pattern(regexp = "^https?://.+",
                message = "expiresUrl must start with http:// or https://")
        String expiresUrl
) {
}
