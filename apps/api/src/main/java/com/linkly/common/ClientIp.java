package com.linkly.common;

import jakarta.servlet.http.HttpServletRequest;

/** Best-effort client IP: first X-Forwarded-For hop (behind a proxy) else the socket address. */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
