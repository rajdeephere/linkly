package com.linkly.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Emits click events to Kafka — <b>fire-and-forget, off the request thread</b> (ADR-0004). {@code @Async}
 * means the resolve path never waits on (or is affected by) the analytics stream: an unreachable broker
 * fails here, in a background thread, and the redirect has already returned.
 */
@Component
public class ClickEventPublisher {

    public static final String TOPIC = "link.clicks";

    private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

    private final KafkaTemplate<Object, Object> kafka;

    public ClickEventPublisher(KafkaTemplate<Object, Object> kafka) {
        this.kafka = kafka;
    }

    @Async("clickExecutor")
    public void publish(ClickEventMessage msg) {
        try {
            kafka.send(TOPIC, msg.code(), msg);
        } catch (RuntimeException analyticsDown) {
            // Analytics must never break redirects — swallow + log.
            log.warn("dropped click event for {}: {}", msg.code(), analyticsDown.getMessage());
        }
    }
}
