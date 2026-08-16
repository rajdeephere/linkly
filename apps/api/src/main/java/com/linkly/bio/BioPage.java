package com.linkly.bio;

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

/** A link-in-bio page (Linktree-style), owned by a workspace, served publicly at {@code /bio/{slug}}. */
@Entity
@Table(name = "bio_page")
@Getter
@Setter
public class BioPage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column
    private String title;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    private String theme = "default";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
