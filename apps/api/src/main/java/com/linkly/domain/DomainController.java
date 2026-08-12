package com.linkly.domain;

import com.linkly.domain.dto.CreateDomainRequest;
import com.linkly.domain.dto.DomainResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/domains")
public class DomainController {

    private final DomainService domains;

    public DomainController(DomainService domains) {
        this.domains = domains;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DomainResponse add(@Valid @RequestBody CreateDomainRequest request) {
        return domains.add(request);
    }

    @GetMapping("/{id}")
    public DomainResponse get(@PathVariable String id) {
        return domains.get(id);
    }

    @PostMapping("/{id}/verify")
    public DomainResponse verify(@PathVariable String id) {
        return domains.verify(id);
    }

    /** Dev/simulation only — stands in for the tenant adding the TXT record at their DNS provider. */
    @PostMapping("/{id}/dns/simulate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void simulateDns(@PathVariable String id) {
        domains.simulateDns(id);
    }
}
