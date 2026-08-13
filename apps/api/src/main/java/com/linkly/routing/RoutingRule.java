package com.linkly.routing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A resolve-time routing rule for a link (ADR-0010). Type: DEVICE | OS | GEO | AB. */
@Entity
@Table(name = "routing_rule")
@Getter
@Setter
public class RoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(nullable = false)
    private String type;

    /** Match target (e.g. "Mobile", "iOS", "IN,BD"); null for AB. */
    @Column(name = "match_value")
    private String matchValue;

    @Column(name = "destination_url", nullable = false, length = 2048)
    private String destinationUrl;

    @Column(nullable = false)
    private int weight = 1;

    @Column(nullable = false)
    private int priority = 100;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
