package com.linkly.analytics;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class AnalyticsConfig {

    /** The click-event topic (produced by the resolver, consumed here). */
    public static final String CLICKS_TOPIC = "link.clicks";

    /** Auto-create the clicks topic on startup (doesn't rely on broker auto-create). */
    @Bean
    public NewTopic clicksTopic() {
        return TopicBuilder.name(CLICKS_TOPIC).partitions(3).replicas(1).build();
    }
}
