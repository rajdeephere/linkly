package com.linkly.analytics.dto;

import java.util.List;

/** Aggregated analytics for one link over the last {@code days}. */
public record AnalyticsResponse(
        String code,
        long totalClicks,
        long humanClicks,
        long botClicks,
        int days,
        List<Point> timeseries,
        List<Bucket> byDevice,
        List<Bucket> byBrowser,
        List<Bucket> byCountry,
        List<Bucket> byReferrer
) {
    /** A point on the daily time-series. */
    public record Point(String date, long count) {
    }

    /** A category → count bucket (device, browser, country, referrer). */
    public record Bucket(String label, long count) {
    }
}
