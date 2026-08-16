package com.linkly.bio;

import com.linkly.auth.AppUserPrincipal;
import com.linkly.bio.dto.BioDtos.AddBlockRequest;
import com.linkly.bio.dto.BioDtos.BioResponse;
import com.linkly.bio.dto.BioDtos.CreateBioRequest;
import com.linkly.bio.dto.BioDtos.ReorderBlocksRequest;
import com.linkly.bio.dto.BioDtos.UpdateBioRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Management API for link-in-bio pages — workspace-scoped. */
@RestController
@RequestMapping("/v1/bio")
public class BioController {

    private final BioService bio;

    public BioController(BioService bio) {
        this.bio = bio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BioResponse create(@Valid @RequestBody CreateBioRequest req,
                              @AuthenticationPrincipal AppUserPrincipal me) {
        return bio.create(req, me.workspaceId());
    }

    @GetMapping
    public List<BioResponse> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return bio.list(me.workspaceId());
    }

    @GetMapping("/{id}")
    public BioResponse get(@PathVariable String id, @AuthenticationPrincipal AppUserPrincipal me) {
        return bio.get(id, me.workspaceId());
    }

    @PatchMapping("/{id}")
    public BioResponse update(@PathVariable String id, @Valid @RequestBody UpdateBioRequest req,
                              @AuthenticationPrincipal AppUserPrincipal me) {
        return bio.update(id, me.workspaceId(), req);
    }

    @PostMapping("/{id}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public BioResponse addBlock(@PathVariable String id, @Valid @RequestBody AddBlockRequest req,
                                @AuthenticationPrincipal AppUserPrincipal me) {
        return bio.addBlock(id, me.workspaceId(), req);
    }

    @DeleteMapping("/{id}/blocks/{blockId}")
    public BioResponse deleteBlock(@PathVariable String id, @PathVariable String blockId,
                                   @AuthenticationPrincipal AppUserPrincipal me) {
        return bio.deleteBlock(id, me.workspaceId(), blockId);
    }

    @PutMapping("/{id}/blocks/order")
    public BioResponse reorderBlocks(@PathVariable String id,
                                     @Valid @RequestBody ReorderBlocksRequest req,
                                     @AuthenticationPrincipal AppUserPrincipal me) {
        return bio.reorder(id, me.workspaceId(), req.order());
    }
}
