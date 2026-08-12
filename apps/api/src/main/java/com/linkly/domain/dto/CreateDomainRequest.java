package com.linkly.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Add a custom domain to serve links on. */
public record CreateDomainRequest(
        @NotBlank(message = "hostname is required")
        @Pattern(regexp = "^(?!-)[A-Za-z0-9-]{1,63}(\\.[A-Za-z0-9-]{1,63})+$",
                message = "hostname must be a valid domain (e.g. go.acme.com)")
        String hostname
) {
}
