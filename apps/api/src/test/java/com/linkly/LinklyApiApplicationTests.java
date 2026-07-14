package com.linkly;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context-load smoke test. Requires the local infra to be up
 * (docker compose -f infra/docker-compose.yml up -d) since it connects to Postgres.
 */
@SpringBootTest
class LinklyApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
