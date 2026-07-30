package com.linkly.analytics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes raw click events and writes enriched rows to Postgres — the async side of the pipeline
 * (ADR-0004). Runs in its own consumer group, so it can lag, restart, or be scaled independently of the
 * resolver; events that arrive while it's down wait in Kafka and are drained on restart (offset-based).
 */
@Component
public class ClickEventConsumer {

    private final ClickEventRepository repo;
    private final UserAgentParser userAgents;

    public ClickEventConsumer(ClickEventRepository repo, UserAgentParser userAgents) {
        this.repo = repo;
        this.userAgents = userAgents;
    }

    @KafkaListener(topics = ClickEventPublisher.TOPIC, groupId = "linkly-analytics")
    public void onClick(ClickEventMessage m) {
        UserAgentParser.Result ua = userAgents.parse(m.userAgent());

        ClickEvent e = new ClickEvent();
        e.setLinkCode(m.code());
        e.setTs(OffsetDateTime.ofInstant(Instant.ofEpochMilli(m.ts()), ZoneOffset.UTC));
        e.setIpHash(hash(m.ip()));      // GDPR: never store the raw IP
        e.setCountry(m.country());
        e.setDevice(ua.device());
        e.setOs(ua.os());
        e.setBrowser(ua.browser());
        e.setReferer(m.referer());
        e.setBot(ua.bot());

        repo.save(e);
    }

    private static String hash(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(ip.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
