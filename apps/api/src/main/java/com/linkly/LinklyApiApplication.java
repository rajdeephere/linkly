package com.linkly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Linkly management API — auth, links, domains, teams, analytics queries.
 * The redirect resolver is a separate service (see docs/ARCHITECTURE.md, ADR-1).
 */
@SpringBootApplication
public class LinklyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinklyApiApplication.class, args);
    }
}
