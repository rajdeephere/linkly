package com.linkly.resolver;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainRepository extends JpaRepository<Domain, UUID> {

    Optional<Domain> findByHostname(String hostname);
}
