package com.linkly.link;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class QrController {

    private final LinkService links;
    private final QrService qr;

    public QrController(LinkService links, QrService qr) {
        this.links = links;
        this.qr = qr;
    }

    /** PNG QR code for a link's short URL. {@code size} is clamped to a sane range. */
    @GetMapping(value = "/v1/links/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String id,
                                     @RequestParam(defaultValue = "300") int size) {
        Link link = links.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "link not found"));
        int px = Math.max(100, Math.min(1000, size));
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.IMAGE_PNG)
                .body(qr.png(links.shortUrl(link), px));
    }
}
