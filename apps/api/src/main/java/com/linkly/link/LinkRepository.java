package com.linkly.link;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, UUID> {

    /** Alias/code availability is per-domain — uniqueness is (domain_id, code). */
    boolean existsByDomainIdAndCode(UUID domainId, String code);
}
