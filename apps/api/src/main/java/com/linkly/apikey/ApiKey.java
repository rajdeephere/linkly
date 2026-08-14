package com.linkly.apikey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A programmatic access key for a workspace. Only the hash is stored (plaintext shown once). */
@Entity
@Table(name = "api_key")
@Getter
@Setter
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column
    private String name;

    @Column(nullable = false)
    private String prefix;

    @Column(name = "hashed_key", nullable = false, unique = true)
    private String hashedKey;

    @Column(nullable = false)
    private String role = "member";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;
}
