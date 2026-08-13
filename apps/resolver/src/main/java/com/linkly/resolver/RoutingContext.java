package com.linkly.resolver;

/**
 * The per-request signals routing rules match on. {@code device}/{@code os} are parsed from the UA;
 * {@code country} comes from the edge (CF-IPCountry / x-vercel-ip-country); {@code bucketKey} is a stable
 * per-visitor value (ip+ua) for deterministic A-B bucketing.
 */
public record RoutingContext(String device, String os, String country, String bucketKey) {

    static RoutingContext from(String userAgent, String country, String ip) {
        String ua = userAgent == null ? "" : userAgent.toLowerCase();
        String os = ua.contains("android") ? "Android"
                : (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) ? "iOS"
                : ua.contains("windows") ? "Windows"
                : (ua.contains("mac os") || ua.contains("macintosh")) ? "macOS"
                : ua.contains("linux") ? "Linux" : "Unknown";
        String device = (ua.contains("ipad") || ua.contains("tablet")) ? "Tablet"
                : (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) ? "Mobile"
                : "Desktop";
        return new RoutingContext(device, os, country, (ip == null ? "" : ip) + "|" + ua);
    }
}
