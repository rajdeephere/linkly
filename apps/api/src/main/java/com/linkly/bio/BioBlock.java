package com.linkly.bio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A single link on a bio page. */
@Entity
@Table(name = "bio_block")
@Getter
@Setter
public class BioBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bio_page_id", nullable = false)
    private UUID bioPageId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private int position;
}
