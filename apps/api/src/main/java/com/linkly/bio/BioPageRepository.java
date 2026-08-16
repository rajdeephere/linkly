package com.linkly.bio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BioPageRepository extends JpaRepository<BioPage, UUID> {

    Optional<BioPage> findBySlug(String slug);

    Optional<BioPage> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    List<BioPage> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    boolean existsBySlug(String slug);
}
