package com.linkly.web;

import com.linkly.link.LinkRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trivial liveness + smoke-test endpoint for Day 1.
 * Confirms the app is up and can reach Postgres (via a count query).
 * Real health lives at {@code /actuator/health}.
 */
@RestController
public class PingController {

    private final LinkRepository linkRepository;

    public PingController(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "app", "linkly-api",
                "status", "ok",
                "seededLinks", linkRepository.count()
        );
    }
}
