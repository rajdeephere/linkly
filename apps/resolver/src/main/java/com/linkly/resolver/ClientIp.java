package com.linkly.resolver;

import jakarta.servlet.http.HttpServletRequest;

final class ClientIp {

    private ClientIp() {
    }

    static String of(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
