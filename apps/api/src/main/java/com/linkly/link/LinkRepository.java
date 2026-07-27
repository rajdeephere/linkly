package com.linkly.link;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, UUID> {

    Optional<Link> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Atomically bump the click count, but only while the link is under its limit. Returns the number
     * of rows updated: 1 = counted (proceed), 0 = at/over the cap (or gone). Doing the check and the
     * increment in one statement makes the click-cap race-free under concurrent resolves.
     */
    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + 1 "
            + "where l.id = :id and (l.clickLimit is null or l.clickCount < l.clickLimit)")
    int tryIncrementClick(@Param("id") UUID id);
}
