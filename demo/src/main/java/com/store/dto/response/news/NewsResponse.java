package com.store.dto.response.news;

import com.store.entity.news.News;
import com.store.entity.news.NewsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse {

    private Long newsId;
    private Integer newsCatId;
    private String categoryName;
    private String categorySlug;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String summary;
    private String content;
    private Long authorId;
    private String authorName;
    private Integer viewCount;
    private NewsStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NewsResponse fromEntity(News news) {
        if (news == null) {
            return null;
        }
        return NewsResponse.builder()
                .newsId(news.getNewsId())
                .newsCatId(news.getCategory() != null ? news.getCategory().getNewsCatId() : null)
                .categoryName(news.getCategory() != null ? news.getCategory().getName() : null)
                .categorySlug(news.getCategory() != null ? news.getCategory().getSlug() : null)
                .title(news.getTitle())
                .slug(news.getSlug())
                .thumbnailUrl(news.getThumbnailUrl())
                .summary(news.getSummary())
                .content(news.getContent())
                .authorId(news.getAuthor() != null ? news.getAuthor().getUserId() : null)
                .authorName(news.getAuthor() != null ? news.getAuthor().getFullName() : null)
                .viewCount(news.getViewCount())
                .status(news.getStatus())
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}
