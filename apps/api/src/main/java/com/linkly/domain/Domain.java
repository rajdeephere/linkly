package com.linkly.domain;

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

/**
 * A hostname links can be served on. The seeded {@code is_default} row is the app's own short host;
 * tenants add their own (e.g. {@code go.acme.com}), prove ownership via DNS, then get per-host TLS.
 */
@Entity
@Table(name = "domain")
@Getter
@Setter
public class Domain {

    /** The seeded default domain (app's own host) — links with no custom domain belong here. */
    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d0");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, unique = true)
    private String hostname;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    /** pending | active | failed */
    @Column(name = "tls_status", nullable = false)
    private String tlsStatus = "pending";

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
