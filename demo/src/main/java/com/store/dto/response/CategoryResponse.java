package com.store.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.store.entity.category.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
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
    private List<CategoryResponse> children;

    public static CategoryResponse fromEntity(Category category) {
        if (category == null) {
            return null;
        }

        Integer parentId = null;
        String parentName = null;
        if (category.getParent() != null) {
            parentId = category.getParent().getCategoryId();
            parentName = category.getParent().getName();
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
                .children(new ArrayList<>())
                .build();
    }
}
