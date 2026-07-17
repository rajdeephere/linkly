package com.linkly.common;

import java.time.OffsetDateTime;
import java.util.Map;

/** Uniform error envelope returned by {@link GlobalExceptionHandler}. */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
