package com.linkly.workspace;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    /** The caller's membership (MVP: one workspace per user, created at registration). */
    Optional<Membership> findFirstByUserId(UUID userId);
}
