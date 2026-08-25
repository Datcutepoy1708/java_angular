package com.store.dto.response.news;

import com.store.entity.news.NewsCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsCategoryResponse {

    private Integer newsCatId;
    private String name;
    private String slug;
    private String description;
    private Integer sortOrder;
    private String status;

    public static NewsCategoryResponse fromEntity(NewsCategory category) {
        if (category == null) {
            return null;
        }
        return NewsCategoryResponse.builder()
                .newsCatId(category.getNewsCatId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .build();
    }
}
