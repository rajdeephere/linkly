package com.linkly.auth;

import java.util.UUID;

/** The authenticated caller — carries the workspace + role for scoping and RBAC. */
public record AppUserPrincipal(UUID userId, UUID workspaceId, String role, String email) {

    public boolean canManage() {
        return "owner".equals(role) || "admin".equals(role);
    }
}
