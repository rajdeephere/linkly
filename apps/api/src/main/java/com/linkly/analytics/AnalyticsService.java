package com.linkly.analytics;

import com.linkly.analytics.dto.AnalyticsResponse;
import com.linkly.analytics.dto.AnalyticsResponse.Bucket;
import com.linkly.analytics.dto.AnalyticsResponse.Point;
import com.linkly.link.Link;
import com.linkly.link.LinkService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Reads aggregated click analytics for a link (Postgres now; ClickHouse later — ADR-0007). */
@Service
public class AnalyticsService {

    private final LinkService links;
    private final ClickEventRepository clicks;

    public AnalyticsService(LinkService links, ClickEventRepository clicks) {
        this.links = links;
        this.clicks = clicks;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse forLink(String id, java.util.UUID workspaceId, int days) {
        Link link = links.findById(id, workspaceId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
        String code = link.getCode();
        OffsetDateTime since = OffsetDateTime.now().minusDays(Math.max(1, days));

        Object[] totals = clicks.totals(code, since).get(0);
        long humans = num(totals[0]);
        long bots = num(totals[1]);

        return new AnalyticsResponse(
                code,
                humans + bots,
                humans,
                bots,
                days,
                clicks.timeseriesDaily(code, since).stream()
                        .map(r -> new Point((String) r[0], num(r[1]))).toList(),
                buckets(clicks.byDevice(code, since)),
                buckets(clicks.byBrowser(code, since)),
                buckets(clicks.byCountry(code, since)),
                buckets(clicks.byReferrer(code, since)));
    }

    private static List<Bucket> buckets(List<Object[]> rows) {
        return rows.stream().map(r -> new Bucket((String) r[0], num(r[1]))).toList();
    }

    private static long num(Object o) {
        return ((Number) o).longValue();
    }
}
