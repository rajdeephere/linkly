package com.linkly.analytics;

/**
 * Raw click payload put on Kafka by the resolver (fire-and-forget). Enrichment (IP hashing,
 * UA parsing, bot flag) happens in the consumer, off the hot path. {@code ts} is epoch millis to keep
 * JSON serialization trivial.
 */
public record ClickEventMessage(
        String code,
        String ip,
        String userAgent,
        String referer,
        String country,
        long ts
) {
}
