package com.linkly.analytics;

import org.springframework.stereotype.Component;

/**
 * Lightweight heuristic User-Agent parser — device / OS / browser + a bot flag. Deliberately
 * dependency-free for the MVP; a production build would swap in a maintained UA library (e.g. uap-java)
 * for accuracy against the long tail.
 */
@Component
public class UserAgentParser {

    public record Result(String device, String os, String browser, boolean bot) {
    }

    public Result parse(String rawUa) {
        String ua = rawUa == null ? "" : rawUa.toLowerCase();

        boolean bot = ua.isBlank()
                || ua.matches(".*(bot|crawl|spider|slurp|bingpreview|facebookexternalhit|monitor).*");

        String os = ua.contains("android") ? "Android"
                : (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) ? "iOS"
                : ua.contains("windows") ? "Windows"
                : (ua.contains("mac os") || ua.contains("macintosh")) ? "macOS"
                : ua.contains("linux") ? "Linux"
                : "Unknown";

        String device = (ua.contains("ipad") || ua.contains("tablet")) ? "Tablet"
                : (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) ? "Mobile"
                : "Desktop";

        String browser = ua.contains("edg") ? "Edge"
                : ua.contains("firefox") ? "Firefox"
                : (ua.contains("chrome") || ua.contains("crios")) ? "Chrome"
                : ua.contains("safari") ? "Safari"
                : "Other";

        return new Result(device, os, browser, bot);
    }
}
