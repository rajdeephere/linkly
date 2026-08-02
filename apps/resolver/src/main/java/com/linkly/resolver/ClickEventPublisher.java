package com.linkly.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Fire-and-forget click emission, off the request thread (ADR-0004). */
@Component
public class ClickEventPublisher {

    public static final String TOPIC = "link.clicks";

    private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

    private final KafkaTemplate<Object, Object> kafka;

    public ClickEventPublisher(KafkaTemplate<Object, Object> kafka) {
        this.kafka = kafka;
    }

    @Async
    public void publish(ClickEventMessage msg) {
        try {
            kafka.send(TOPIC, msg.code(), msg);
        } catch (RuntimeException analyticsDown) {
            log.warn("dropped click event for {}: {}", msg.code(), analyticsDown.getMessage());
        }
    }
}
