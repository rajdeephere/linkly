package com.linkly.resolver;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Picks a destination from a link's routing rules and the request context (ADR-0010).
 * <ol>
 *   <li>DEVICE/OS/GEO rules, lowest {@code priority} first — the first match wins (deep links).</li>
 *   <li>else weighted A-B rules — the visitor is bucketed <b>deterministically</b> by {@code bucketKey},
 *       so a returning visitor always lands on the same variant.</li>
 *   <li>else the link's default destination.</li>
 * </ol>
 */
@Component
public class RoutingEvaluator {

    public String pick(List<RoutingRule> rules, RoutingContext ctx, String defaultUrl) {
        String matched = rules.stream()
                .filter(r -> !"AB".equals(r.getType()))
                .sorted(Comparator.comparingInt(RoutingRule::getPriority))
                .filter(r -> matches(r, ctx))
                .map(RoutingRule::getDestinationUrl)
                .findFirst()
                .orElse(null);
        if (matched != null) {
            return matched;
        }

        List<RoutingRule> ab = rules.stream().filter(r -> "AB".equals(r.getType())).toList();
        return ab.isEmpty() ? defaultUrl : bucket(ab, ctx.bucketKey());
    }

    private boolean matches(RoutingRule rule, RoutingContext ctx) {
        if (rule.getMatchValue() == null) {
            return false;
        }
        return switch (rule.getType()) {
            case "DEVICE" -> rule.getMatchValue().equalsIgnoreCase(ctx.device());
            case "OS" -> rule.getMatchValue().equalsIgnoreCase(ctx.os());
            case "GEO" -> ctx.country() != null && List.of(rule.getMatchValue().toUpperCase().split(","))
                    .contains(ctx.country().toUpperCase());
            default -> false;
        };
    }

    /** Deterministic weighted bucketing: same key → same variant every time (sticky A-B). */
    private String bucket(List<RoutingRule> ab, String key) {
        int total = ab.stream().mapToInt(RoutingRule::getWeight).sum();
        if (total <= 0) {
            return ab.get(0).getDestinationUrl();
        }
        int point = Math.floorMod(key.hashCode(), total);
        int cumulative = 0;
        for (RoutingRule r : ab) {
            cumulative += r.getWeight();
            if (point < cumulative) {
                return r.getDestinationUrl();
            }
        }
        return ab.get(ab.size() - 1).getDestinationUrl();
    }
}
