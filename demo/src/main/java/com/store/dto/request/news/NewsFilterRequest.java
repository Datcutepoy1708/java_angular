package com.store.dto.request.news;

import com.store.entity.news.NewsStatus;
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
public class NewsFilterRequest {

    private Integer categoryId;
    private NewsStatus status;
    private String keyword;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 10;

    @Builder.Default
    private String sortBy = "publishedAt";

    @Builder.Default
    private String sortDir = "desc";
}
