package com.linkly.resolver;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, UUID> {

    Optional<Link> findByCode(String code);

    /** Race-free click-cap: increments only while under the limit; 0 rows = capped. */
    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + 1 "
            + "where l.id = :id and (l.clickLimit is null or l.clickCount < l.clickLimit)")
    int tryIncrementClick(@Param("id") UUID id);
}
