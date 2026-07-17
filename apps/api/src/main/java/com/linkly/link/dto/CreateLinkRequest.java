package com.linkly.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to create a short link. Day 2: destination + optional title.
 * (Custom alias, expiry, domain, and routing rules arrive in later phases.)
 */
public record CreateLinkRequest(
        @NotBlank(message = "destinationUrl is required")
        @Pattern(regexp = "^https?://.+",
                message = "destinationUrl must start with http:// or https://")
        String destinationUrl,

        @Size(max = 255, message = "title must be at most 255 characters")
        String title
) {
}
