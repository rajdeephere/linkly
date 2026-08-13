package com.linkly.routing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Add a routing rule to a link. {@code matchValue} is required for DEVICE/OS/GEO, ignored for AB. */
public record CreateRuleRequest(
        @Pattern(regexp = "DEVICE|OS|GEO|AB", message = "type must be DEVICE, OS, GEO, or AB")
        String type,

        String matchValue,

        @NotBlank @Pattern(regexp = "^https?://.+",
                message = "destinationUrl must start with http:// or https://")
        String destinationUrl,

        @Positive Integer weight,
        Integer priority
) {
}
