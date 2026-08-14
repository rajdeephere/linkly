package com.linkly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Auth request/response DTOs. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @Size(max = 255) String name,
            @NotBlank @Size(min = 8, max = 100, message = "password must be 8–100 characters")
            String password) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record AuthResponse(String token, UUID userId, UUID workspaceId, String role, String email) {
    }
}
