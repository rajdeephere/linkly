package com.linkly.bio;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BioBlockRepository extends JpaRepository<BioBlock, UUID> {

    List<BioBlock> findByBioPageIdOrderByPositionAsc(UUID bioPageId);
}
