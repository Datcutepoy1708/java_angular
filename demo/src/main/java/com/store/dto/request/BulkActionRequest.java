package com.store.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkActionRequest {

    @NotNull
    @NotEmpty
    private List<Long> ids;

    /**
     * Supported actions: "delete" | "restore" | "activate" | "deactivate"
     */
    @NotNull
    private String action;
}
