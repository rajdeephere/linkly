package com.linkly.resolver;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/** Read view of a routing rule, evaluated at resolve time (ADR-0010). */
@Entity
@Table(name = "routing_rule")
@Getter
public class RoutingRule {

    @Id
    private UUID id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(nullable = false)
    private String type;              // DEVICE | OS | GEO | AB

    @Column(name = "match_value")
    private String matchValue;

    @Column(name = "destination_url", nullable = false)
    private String destinationUrl;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false)
    private int priority;
}
