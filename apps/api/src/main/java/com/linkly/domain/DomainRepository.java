package com.linkly.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainRepository extends JpaRepository<Domain, UUID> {

    Optional<Domain> findByHostname(String hostname);

    boolean existsByHostname(String hostname);

    List<Domain> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
