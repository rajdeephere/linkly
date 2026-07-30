package com.linkly.analytics;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

    long countByLinkCode(String linkCode);

    /** Human/bot split for a link since a cutoff. One row: [humans, bots]. */
    @Query(value = """
            select count(*) filter (where not is_bot) as humans,
                   count(*) filter (where is_bot)     as bots
            from click_event
            where link_code = :code and ts >= :since
            """, nativeQuery = true)
    List<Object[]> totals(@Param("code") String code, @Param("since") OffsetDateTime since);

    /** Clicks per day: rows of [YYYY-MM-DD, count]. */
    @Query(value = """
            select to_char(date_trunc('day', ts), 'YYYY-MM-DD') as day, count(*) as c
            from click_event
            where link_code = :code and ts >= :since
            group by day order by day
            """, nativeQuery = true)
    List<Object[]> timeseriesDaily(@Param("code") String code, @Param("since") OffsetDateTime since);

    @Query(value = """
            select coalesce(device, 'Unknown') as k, count(*) as c
            from click_event where link_code = :code and ts >= :since
            group by k order by c desc
            """, nativeQuery = true)
    List<Object[]> byDevice(@Param("code") String code, @Param("since") OffsetDateTime since);

    @Query(value = """
            select coalesce(browser, 'Unknown') as k, count(*) as c
            from click_event where link_code = :code and ts >= :since
            group by k order by c desc
            """, nativeQuery = true)
    List<Object[]> byBrowser(@Param("code") String code, @Param("since") OffsetDateTime since);

    @Query(value = """
            select coalesce(country, 'Unknown') as k, count(*) as c
            from click_event where link_code = :code and ts >= :since
            group by k order by c desc
            """, nativeQuery = true)
    List<Object[]> byCountry(@Param("code") String code, @Param("since") OffsetDateTime since);

    @Query(value = """
            select coalesce(nullif(referer, ''), 'direct') as k, count(*) as c
            from click_event where link_code = :code and ts >= :since
            group by k order by c desc limit 10
            """, nativeQuery = true)
    List<Object[]> byReferrer(@Param("code") String code, @Param("since") OffsetDateTime since);
}
