package com.store.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkActionResult {

    private int successCount;
    private int failCount;
    private List<BulkItemResult> results;

    @Data
    @Builder
    public static class BulkItemResult {
        private long id;
        private boolean success;
        private String error; // "NOT_FOUND", "ALREADY_DELETED", "ALREADY_ACTIVE", etc.
    }
}
