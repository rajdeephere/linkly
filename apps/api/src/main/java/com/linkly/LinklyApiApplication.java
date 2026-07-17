package com.linkly;

import com.linkly.config.LinklyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Linkly management API — auth, links, domains, teams, analytics queries.
 * The redirect resolver is a separate service (see docs/ARCHITECTURE.md, ADR-1).
 */
@SpringBootApplication
@EnableConfigurationProperties(LinklyProperties.class)
public class LinklyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinklyApiApplication.class, args);
    }
}
