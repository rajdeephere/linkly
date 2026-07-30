package com.linkly.analytics;

import com.linkly.analytics.dto.AnalyticsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    /** Aggregated analytics for a link over the last {@code days} (default 30). */
    @GetMapping("/v1/links/{id}/analytics")
    public AnalyticsResponse analytics(@PathVariable String id,
                                       @RequestParam(defaultValue = "30") int days) {
        return analytics.forLink(id, days);
    }
}
