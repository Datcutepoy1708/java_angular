package com.store.dto.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroupResponse {

    private String groupCode;
    private String groupName;
    @Builder.Default
    private List<PermissionResponse> permissions = new ArrayList<>();
}
