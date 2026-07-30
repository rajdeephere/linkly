package com.linkly.analytics;

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

/** One resolved click — the append-only analytics fact (enriched; IP stored hashed). */
@Entity
@Table(name = "click_event")
@Getter
@Setter
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "link_code", nullable = false)
    private String linkCode;

    @Column(nullable = false)
    private OffsetDateTime ts = OffsetDateTime.now();

    @Column(name = "ip_hash")
    private String ipHash;

    @Column
    private String country;

    @Column
    private String device;

    @Column
    private String os;

    @Column
    private String browser;

    @Column(length = 2048)
    private String referer;

    @Column(name = "is_bot", nullable = false)
    private boolean bot;
}
