package com.linkly.resolver;

/**
 * Raw click payload put on Kafka (topic {@code link.clicks}) — same shape as the api's consumer expects
 * (type headers are disabled so it maps to the consumer's own record).
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
