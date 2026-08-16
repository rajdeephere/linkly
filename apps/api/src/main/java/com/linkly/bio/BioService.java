package com.linkly.bio;

import com.linkly.bio.dto.BioDtos.AddBlockRequest;
import com.linkly.bio.dto.BioDtos.BioResponse;
import com.linkly.bio.dto.BioDtos.CreateBioRequest;
import com.linkly.bio.dto.BioDtos.UpdateBioRequest;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

    /** Remove a block, then compact the survivors to positions 0..n-1 so there are no gaps. */
    @Transactional
    public BioResponse deleteBlock(String id, UUID workspaceId, String blockId) {
        BioPage p = require(id, workspaceId);
        BioBlock target = requireBlock(blockId, p.getId());
        blocks.delete(target);
        List<BioBlock> remaining = blocks.findByBioPageIdOrderByPositionAsc(p.getId());
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        blocks.saveAll(remaining);
        return response(p);
    }

    /**
     * Reorder a page's blocks. The request must be a permutation of exactly the page's current blocks —
     * reject anything else so the UI can't drop or smuggle in a block. New position = index in the list.
     */
    @Transactional
    public BioResponse reorder(String id, UUID workspaceId, List<UUID> order) {
        BioPage p = require(id, workspaceId);
        List<BioBlock> current = blocks.findByBioPageIdOrderByPositionAsc(p.getId());
        Map<UUID, BioBlock> byId = current.stream()
                .collect(Collectors.toMap(BioBlock::getId, b -> b));
        if (order.size() != current.size() || !byId.keySet().equals(new HashSet<>(order))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "order must be a permutation of this page's blocks");
        }
        for (int i = 0; i < order.size(); i++) {
            byId.get(order.get(i)).setPosition(i);
        }
        blocks.saveAll(byId.values());
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

    /** Load a block and confirm it belongs to this page (never leak another page's block). */
    private BioBlock requireBlock(String blockId, UUID bioPageId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(blockId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "block not found");
        }
        BioBlock b = blocks.findById(uuid).orElse(null);
        if (b == null || !b.getBioPageId().equals(bioPageId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "block not found");
        }
        return b;
    }

    private BioResponse response(BioPage p) {
        return BioResponse.of(p, blocks.findByBioPageIdOrderByPositionAsc(p.getId()));
    }
}
