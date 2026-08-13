package com.linkly.routing.dto;

import com.linkly.routing.RoutingRule;
import java.util.UUID;

public record RuleResponse(
        UUID id, String type, String matchValue, String destinationUrl, int weight, int priority) {

    public static RuleResponse from(RoutingRule r) {
        return new RuleResponse(r.getId(), r.getType(), r.getMatchValue(),
                r.getDestinationUrl(), r.getWeight(), r.getPriority());
    }
}
