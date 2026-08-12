package com.linkly.resolver;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

/** Read view of a domain — used to resolve a request's Host header to a domain id. */
@Entity
@Table(name = "domain")
@Getter
public class Domain {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String hostname;
}
