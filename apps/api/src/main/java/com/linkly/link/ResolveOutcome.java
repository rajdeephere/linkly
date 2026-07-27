package com.linkly.link;

/**
 * Result of resolving a code. Keeps the resolve *decision* (redirect / gone / not-found) in the service
 * and lets the controller map it to HTTP without re-deriving anything.
 */
public record ResolveOutcome(Status status, String url) {

    public enum Status { REDIRECT, GONE, NOT_FOUND }

    public static ResolveOutcome redirect(String url) {
        return new ResolveOutcome(Status.REDIRECT, url);
    }

    public static ResolveOutcome gone() {
        return new ResolveOutcome(Status.GONE, null);
    }

    public static ResolveOutcome notFound() {
        return new ResolveOutcome(Status.NOT_FOUND, null);
    }
}
