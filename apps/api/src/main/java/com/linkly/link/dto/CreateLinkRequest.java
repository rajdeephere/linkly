package com.linkly.link.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Request to create a short link.
 * Day 2: destination + title. Day 4: optional custom alias, expiry (date + click cap), and a fallback
 * URL for expired/capped links.
 */
public record CreateLinkRequest(
        @NotBlank(message = "destinationUrl is required")
        @Pattern(regexp = "^https?://.+",
                message = "destinationUrl must start with http:// or https://")
        String destinationUrl,

        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        /** Custom domain to create the link on; null → the default domain. */
        String domainId,

        @Pattern(regexp = "^[0-9A-Za-z]{1,64}$",
                message = "alias must be 1–64 base62 characters")
        String alias,

        @Future(message = "expiresAt must be in the future")
        OffsetDateTime expiresAt,

        @Positive(message = "clickLimit must be positive")
        Long clickLimit,

        @Pattern(regexp = "^https?://.+",
                message = "expiresUrl must start with http:// or https://")
        String expiresUrl
) {
    /** A blank alias (empty string from the form) means "no alias — generate one". */
    public boolean hasAlias() {
        return alias != null && !alias.isBlank();
    }
}
