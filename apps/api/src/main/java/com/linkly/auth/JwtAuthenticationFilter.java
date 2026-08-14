package com.linkly.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates the Bearer JWT and populates the SecurityContext with an {@link AppUserPrincipal}. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthenticationFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // An earlier filter (API key) may already have authenticated — don't clobber it.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Jws<Claims> jws = jwt.parse(header.substring(7));
                Claims c = jws.getPayload();
                AppUserPrincipal principal = new AppUserPrincipal(
                        UUID.fromString(c.getSubject()),
                        UUID.fromString(c.get("wsid", String.class)),
                        c.get("role", String.class),
                        c.get("email", String.class));
                var auth = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (RuntimeException invalid) {
                SecurityContextHolder.clearContext(); // bad/expired token → stays unauthenticated
            }
        }
        chain.doFilter(req, res);
    }
}
