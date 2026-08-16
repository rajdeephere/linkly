package com.linkly.bio.dto;

import com.linkly.bio.BioBlock;
import com.linkly.bio.BioPage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class BioDtos {

    private BioDtos() {
    }

    public record CreateBioRequest(
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,63}$",
                    message = "slug must be 2–64 chars: lowercase letters, digits, hyphens")
            String slug,
            @Size(max = 255) String title,
            @Size(max = 2048) String avatarUrl,
            @Size(max = 500) String bio,
            @Size(max = 32) String theme) {
    }

    public record UpdateBioRequest(
            @Size(max = 255) String title,
            @Size(max = 2048) String avatarUrl,
            @Size(max = 500) String bio,
            @Size(max = 32) String theme) {
    }

    public record AddBlockRequest(
            @NotBlank @Size(max = 255) String label,
            @NotBlank @Pattern(regexp = "^https?://.+",
                    message = "url must start with http:// or https://") String url,
            Integer position) {
    }

    /** The blocks of a page in their new display order. Must be a permutation of the page's blocks. */
    public record ReorderBlocksRequest(@NotEmpty List<UUID> order) {
    }

    public record BlockResponse(UUID id, String label, String url, int position) {
        public static BlockResponse from(BioBlock b) {
            return new BlockResponse(b.getId(), b.getLabel(), b.getUrl(), b.getPosition());
        }
    }

    public record BioResponse(UUID id, String slug, String title, String avatarUrl, String bio,
                              String theme, List<BlockResponse> blocks) {
        public static BioResponse of(BioPage p, List<BioBlock> blocks) {
            return new BioResponse(p.getId(), p.getSlug(), p.getTitle(), p.getAvatarUrl(), p.getBio(),
                    p.getTheme(), blocks.stream().map(BlockResponse::from).toList());
        }
    }
}
