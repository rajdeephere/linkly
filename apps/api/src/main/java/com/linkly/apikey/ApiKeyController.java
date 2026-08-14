package com.linkly.apikey;

import com.linkly.apikey.dto.ApiKeyDtos.ApiKeyResponse;
import com.linkly.apikey.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.linkly.apikey.dto.ApiKeyDtos.CreatedApiKey;
import com.linkly.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeys;

    public ApiKeyController(ApiKeyService apiKeys) {
        this.apiKeys = apiKeys;
    }

    /** Create a key. Owner/admin only. The plaintext in the response is shown ONCE. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedApiKey create(@Valid @RequestBody CreateApiKeyRequest request,
                                @AuthenticationPrincipal AppUserPrincipal me) {
        if (!me.canManage()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "requires owner or admin role");
        }
        ApiKeyService.Created created = apiKeys.create(me.workspaceId(), request.name());
        return new CreatedApiKey(created.key().getId(), created.key().getName(),
                created.plaintext(), created.key().getPrefix());
    }

    @GetMapping
    public List<ApiKeyResponse> list(@AuthenticationPrincipal AppUserPrincipal me) {
        return apiKeys.list(me.workspaceId()).stream().map(ApiKeyResponse::from).toList();
    }
}
