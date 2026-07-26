package com.linkly.web;

import com.linkly.link.KeyGenerationService;
import com.linkly.link.LinkRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trivial liveness + smoke-test endpoint. Confirms the app is up, can reach Postgres (via a count
 * query), and reports the KGS pool depth. Real health lives at {@code /actuator/health}.
 */
@RestController
public class PingController {

    private final LinkRepository linkRepository;
    private final KeyGenerationService kgs;

    public PingController(LinkRepository linkRepository, KeyGenerationService kgs) {
        this.linkRepository = linkRepository;
        this.kgs = kgs;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "app", "linkly-api",
                "status", "ok",
                "links", linkRepository.count(),
                "kgsPool", kgs.poolSize()
        );
    }
}
