package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.dto.response.PageResponse;
import com.store.dto.response.news.NewsCategoryResponse;
import com.store.dto.response.news.NewsResponse;
import com.store.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Tag(name = "News & Blog", description = "Public News & Tech Blog Articles APIs")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    @Operation(summary = "Get paginated published news articles with optional category filter (Cached 30m)")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getPublicNews(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size
    ) {
        PageResponse<NewsResponse> response = newsService.getPublicNews(categoryId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin tức thành công", response));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get public news article detail by slug and increment view count")
    public ResponseEntity<ApiResponse<NewsResponse>> getPublicNewsBySlug(@PathVariable String slug) {
        NewsResponse response = newsService.getPublicNewsBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết bài viết thành công", response));
    }

    @GetMapping("/{newsId}/related")
    @Operation(summary = "Get related news articles")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getRelatedNews(
            @PathVariable Long newsId,
            @RequestParam(required = false) Integer categoryId
    ) {
        List<NewsResponse> list = newsService.getRelatedNews(newsId, categoryId);
        return ResponseEntity.ok(ApiResponse.success("Lấy bài viết liên quan thành công", list));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get active news categories")
    public ResponseEntity<ApiResponse<List<NewsCategoryResponse>>> getPublicCategories() {
        List<NewsCategoryResponse> list = newsService.getPublicCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục tin tức thành công", list));
    }
}
