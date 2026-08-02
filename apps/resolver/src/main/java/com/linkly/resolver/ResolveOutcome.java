package com.linkly.resolver;

/**
 * Result of a resolve. {@code cacheable} tells the edge whether it may cache this destination — true
 * only for plain links (no cap/expiry), so the edge never caches a link with per-request state.
 */
public record ResolveOutcome(Status status, String url, boolean cacheable) {

    public enum Status { REDIRECT, GONE, NOT_FOUND }

    public static ResolveOutcome redirect(String url, boolean cacheable) {
        return new ResolveOutcome(Status.REDIRECT, url, cacheable);
    }

    public static ResolveOutcome gone() {
        return new ResolveOutcome(Status.GONE, null, false);
    }

    public static ResolveOutcome notFound() {
        return new ResolveOutcome(Status.NOT_FOUND, null, false);
    }
}
