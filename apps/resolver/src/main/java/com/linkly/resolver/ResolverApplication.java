package com.linkly.resolver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Linkly origin resolver — the read hot path, split out of the management API (ADR-0001) so it scales
 * independently. It shares Postgres/Redis/Kafka with the api but owns only: resolve → 302, cache-aside,
 * and fire-and-forget click emission. The edge (Vercel) sits in front of this (ADR-0003).
 *
 * <p><b>Note:</b> domain classes here are intentionally a lean copy of the api's; the clean refactor is
 * a shared {@code linkly-core} module — deferred to keep this split contained.
 */
@SpringBootApplication
@EnableAsync
public class ResolverApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResolverApplication.class, args);
    }
}
