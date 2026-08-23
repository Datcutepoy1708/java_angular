package com.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.store.entity.category.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse implements Serializable {

    private Integer categoryId;
    private Integer parentId;
    private String parentName;
    private String name;
    private String slug;
    private String iconUrl;
    private String description;
    private Integer sortOrder;
    private String status;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private List<CategoryResponse> children;

    public static CategoryResponse fromEntity(Category category) {
        if (category == null) {
            return null;
        }

        Integer parentId = null;
        String parentName = null;
        if (category.getParent() != null) {
            parentId = category.getParent().getCategoryId();
            // NPE guard: parent may be soft-deleted and filtered out of lookup;
            // show null instead of a name pointing to a deleted category
            parentName = category.getParent().getDeletedAt() == null
                    ? category.getParent().getName()
                    : null;
        }

        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .parentId(parentId)
                .parentName(parentName)
                .name(category.getName())
                .slug(category.getSlug())
                .iconUrl(category.getIconUrl())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus() != null ? category.getStatus().getValue() : null)
                .deleted(category.getDeletedAt() != null)
                .deletedAt(category.getDeletedAt())
                .children(new ArrayList<>())
                .build();
    }
}
