package com.linkly.resolver;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;

/** Read view of a link for the resolve path (schema owned by the api). */
@Entity
@Table(name = "link")
@Getter
public class Link {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "destination_url", nullable = false)
    private String destinationUrl;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "expires_url")
    private String expiresUrl;

    @Column(name = "click_limit")
    private Long clickLimit;

    @Column(name = "click_count", nullable = false)
    private long clickCount;
}
