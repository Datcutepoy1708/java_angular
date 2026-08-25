package com.store.controller;

import com.store.dto.request.news.CreateNewsRequest;
import com.store.dto.request.news.NewsCategoryRequest;
import com.store.dto.request.news.NewsFilterRequest;
import com.store.dto.request.news.UpdateNewsRequest;
import com.store.dto.response.ApiResponse;
import com.store.dto.response.PageResponse;
import com.store.dto.response.news.NewsCategoryResponse;
import com.store.dto.response.news.NewsResponse;
import com.store.entity.news.NewsStatus;
import com.store.security.CustomUserDetails;
import com.store.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
@Tag(name = "Admin News CMS", description = "Admin Tech Blog & News Management APIs")
public class AdminNewsController {

    private final NewsService newsService;

    @GetMapping
    @Operation(summary = "Get paginated news articles for CMS with filters")
    public ResponseEntity<ApiResponse<PageResponse<NewsResponse>>> getAdminNews(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) NewsStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        NewsFilterRequest filter = NewsFilterRequest.builder()
                .categoryId(categoryId)
                .status(status)
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        PageResponse<NewsResponse> response = newsService.getAdminNews(filter);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài viết quản trị thành công", response));
    }

    @GetMapping("/{newsId}")
    @Operation(summary = "Get news article detail by ID for editing")
    public ResponseEntity<ApiResponse<NewsResponse>> getNewsById(@PathVariable Long newsId) {
        NewsResponse response = newsService.getNewsById(newsId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin bài viết thành công", response));
    }

    @PostMapping
    @Operation(summary = "Create news article")
    public ResponseEntity<ApiResponse<NewsResponse>> createNews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateNewsRequest request
    ) {
        NewsResponse response = newsService.createNews(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bài viết thành công", response));
    }

    @PutMapping("/{newsId}")
    @Operation(summary = "Update news article")
    public ResponseEntity<ApiResponse<NewsResponse>> updateNews(
            @PathVariable Long newsId,
            @Valid @RequestBody UpdateNewsRequest request
    ) {
        NewsResponse response = newsService.updateNews(newsId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài viết thành công", response));
    }

    @DeleteMapping("/{newsId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete news article")
    public ResponseEntity<ApiResponse<Void>> deleteNews(@PathVariable Long newsId) {
        newsService.deleteNews(newsId);
        return ResponseEntity.ok(ApiResponse.success("Xóa bài viết thành công", null));
    }

    // Categories
    @GetMapping("/categories")
    @Operation(summary = "Get all news categories for admin")
    public ResponseEntity<ApiResponse<List<NewsCategoryResponse>>> getAdminCategories() {
        List<NewsCategoryResponse> list = newsService.getAdminCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục tin tức quản trị thành công", list));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create news category")
    public ResponseEntity<ApiResponse<NewsCategoryResponse>> createCategory(@Valid @RequestBody NewsCategoryRequest request) {
        NewsCategoryResponse response = newsService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo danh mục tin tức thành công", response));
    }

    @PutMapping("/categories/{catId}")
    @Operation(summary = "Update news category")
    public ResponseEntity<ApiResponse<NewsCategoryResponse>> updateCategory(
            @PathVariable Integer catId,
            @Valid @RequestBody NewsCategoryRequest request
    ) {
        NewsCategoryResponse response = newsService.updateCategory(catId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục tin tức thành công", response));
    }

    @DeleteMapping("/categories/{catId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete news category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Integer catId) {
        newsService.deleteCategory(catId);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục tin tức thành công", null));
    }
}
