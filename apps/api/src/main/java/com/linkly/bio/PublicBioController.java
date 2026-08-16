package com.linkly.bio;

import com.linkly.bio.dto.BioDtos.BioResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Public read of a hosted bio page — no auth (this is the visitor-facing surface). */
@RestController
public class PublicBioController {

    private final BioService bio;

    public PublicBioController(BioService bio) {
        this.bio = bio;
    }

    @GetMapping("/bio/{slug}")
    public BioResponse page(@PathVariable String slug) {
        return bio.publicBySlug(slug);
    }
}
