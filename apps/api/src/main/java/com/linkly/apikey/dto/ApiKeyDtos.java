package com.linkly.apikey.dto;

import com.linkly.apikey.ApiKey;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ApiKeyDtos {

    private ApiKeyDtos() {
    }

    public record CreateApiKeyRequest(@Size(max = 255) String name) {
    }

    /** Returned once on creation — carries the plaintext {@code key}. Never retrievable again. */
    public record CreatedApiKey(UUID id, String name, String key, String prefix) {
    }

    /** Listing view — no secret, just the display prefix. */
    public record ApiKeyResponse(UUID id, String name, String prefix, String role,
                                 OffsetDateTime createdAt) {
        public static ApiKeyResponse from(ApiKey k) {
            return new ApiKeyResponse(k.getId(), k.getName(), k.getPrefix(), k.getRole(),
                    k.getCreatedAt());
        }
    }
}
