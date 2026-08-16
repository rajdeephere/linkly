package com.linkly.bio;

import com.linkly.bio.dto.BioDtos.AddBlockRequest;
import com.linkly.bio.dto.BioDtos.BioResponse;
import com.linkly.bio.dto.BioDtos.CreateBioRequest;
import com.linkly.bio.dto.BioDtos.UpdateBioRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BioService {

    private final BioPageRepository pages;
    private final BioBlockRepository blocks;

    public BioService(BioPageRepository pages, BioBlockRepository blocks) {
        this.pages = pages;
        this.blocks = blocks;
    }

    @Transactional
    public BioResponse create(CreateBioRequest req, UUID workspaceId) {
        String slug = req.slug().toLowerCase();
        if (pages.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "slug already taken");
        }
        BioPage p = new BioPage();
        p.setWorkspaceId(workspaceId);
        p.setSlug(slug);
        p.setTitle(req.title());
        p.setAvatarUrl(req.avatarUrl());
        p.setBio(req.bio());
        if (req.theme() != null && !req.theme().isBlank()) {
            p.setTheme(req.theme());
        }
        return response(pages.save(p));
    }

    @Transactional(readOnly = true)
    public List<BioResponse> list(UUID workspaceId) {
        return pages.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public BioResponse get(String id, UUID workspaceId) {
        return response(require(id, workspaceId));
    }

    @Transactional
    public BioResponse update(String id, UUID workspaceId, UpdateBioRequest req) {
        BioPage p = require(id, workspaceId);
        if (req.title() != null) {
            p.setTitle(req.title());
        }
        if (req.avatarUrl() != null) {
            p.setAvatarUrl(req.avatarUrl());
        }
        if (req.bio() != null) {
            p.setBio(req.bio());
        }
        if (req.theme() != null && !req.theme().isBlank()) {
            p.setTheme(req.theme());
        }
        p.setUpdatedAt(OffsetDateTime.now());
        return response(pages.save(p));
    }

    @Transactional
    public BioResponse addBlock(String id, UUID workspaceId, AddBlockRequest req) {
        BioPage p = require(id, workspaceId);
        BioBlock b = new BioBlock();
        b.setBioPageId(p.getId());
        b.setLabel(req.label());
        b.setUrl(req.url());
        b.setPosition(req.position() == null
                ? blocks.findByBioPageIdOrderByPositionAsc(p.getId()).size() : req.position());
        blocks.save(b);
        return response(p);
    }

    /** Public view by slug — no workspace scoping (this is the hosted page). */
    @Transactional(readOnly = true)
    public BioResponse publicBySlug(String slug) {
        BioPage p = pages.findBySlug(slug.toLowerCase()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "page not found"));
        return response(p);
    }

    private BioPage require(String id, UUID workspaceId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "page not found");
        }
        return pages.findByIdAndWorkspaceId(uuid, workspaceId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "page not found"));
    }

    private BioResponse response(BioPage p) {
        return BioResponse.of(p, blocks.findByBioPageIdOrderByPositionAsc(p.getId()));
    }
}
