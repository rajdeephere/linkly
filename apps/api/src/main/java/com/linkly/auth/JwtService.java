package com.linkly.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues + verifies HS256 JWTs. Self-contained tokens → any api instance authenticates any request. */
@Service
public class JwtService {

    private static final String ISSUER = "linkly";

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
        this.expiryMinutes = props.expiryMinutes();
    }

    public String issue(UUID userId, UUID workspaceId, String role, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .claim("wsid", workspaceId.toString())
                .claim("role", role)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiryMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().requireIssuer(ISSUER).verifyWith(key).build().parseSignedClaims(token);
    }
}
