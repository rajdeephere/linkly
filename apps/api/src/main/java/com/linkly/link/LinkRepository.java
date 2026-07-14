package com.linkly.link;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, UUID> {

    Optional<Link> findByCode(String code);

    boolean existsByCode(String code);
}
