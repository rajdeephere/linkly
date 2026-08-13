package com.linkly.routing;

import com.linkly.routing.dto.CreateRuleRequest;
import com.linkly.routing.dto.RuleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/links/{id}/rules")
public class RoutingController {

    private final RoutingService routing;

    public RoutingController(RoutingService routing) {
        this.routing = routing;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse add(@PathVariable String id, @Valid @RequestBody CreateRuleRequest request) {
        return routing.add(id, request);
    }

    @GetMapping
    public List<RuleResponse> list(@PathVariable String id) {
        return routing.list(id);
    }
}
