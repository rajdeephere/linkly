package com.linkly.link.dto;

import java.util.List;

/** Summary of a bulk CSV import. */
public record BulkResult(int requested, int created, List<BulkError> failed) {

    public record BulkError(int line, String error) {
    }
}
