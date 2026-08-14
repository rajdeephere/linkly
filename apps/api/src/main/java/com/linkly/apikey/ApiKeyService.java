package com.linkly.apikey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues and verifies API keys. Format {@code lk_<40 base62>}; only the SHA-256 hash is persisted. */
@Service
public class ApiKeyService {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String PREFIX = "lk_";

    private final ApiKeyRepository keys;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyService(ApiKeyRepository keys) {
        this.keys = keys;
    }

    public record Created(ApiKey key, String plaintext) {
    }

    /** Create a key for a workspace; the returned plaintext is the ONLY time it's shown. */
    @Transactional
    public Created create(UUID workspaceId, String name) {
        String raw = PREFIX + randomToken(40);
        ApiKey key = new ApiKey();
        key.setWorkspaceId(workspaceId);
        key.setName(name);
        key.setPrefix(raw.substring(0, 10) + "…");
        key.setHashedKey(sha256(raw));
        key.setRole("member");
        return new Created(keys.save(key), raw);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> list(UUID workspaceId) {
        return keys.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    /** Resolve a presented key to its record (for the auth filter). */
    @Transactional(readOnly = true)
    public Optional<ApiKey> authenticate(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(PREFIX)) {
            return Optional.empty();
        }
        return keys.findByHashedKey(sha256(rawKey));
    }

    public static boolean looksLikeApiKey(String token) {
        return token != null && token.startsWith(PREFIX);
    }

    private String randomToken(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
