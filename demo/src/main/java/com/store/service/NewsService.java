package com.store.service;

import com.store.dto.request.news.CreateNewsRequest;
import com.store.dto.request.news.NewsCategoryRequest;
import com.store.dto.request.news.NewsFilterRequest;
import com.store.dto.request.news.UpdateNewsRequest;
import com.store.dto.response.PageResponse;
import com.store.dto.response.news.NewsCategoryResponse;
import com.store.dto.response.news.NewsResponse;

import java.util.List;

public interface NewsService {

    PageResponse<NewsResponse> getPublicNews(Integer categoryId, int page, int size);

    NewsResponse getPublicNewsBySlug(String slug);

    List<NewsResponse> getRelatedNews(Long newsId, Integer categoryId);

    List<NewsCategoryResponse> getPublicCategories();

    PageResponse<NewsResponse> getAdminNews(NewsFilterRequest filter);

    NewsResponse getNewsById(Long newsId);

    NewsResponse createNews(Long authorId, CreateNewsRequest request);

    NewsResponse updateNews(Long newsId, UpdateNewsRequest request);

    void deleteNews(Long newsId);

    List<NewsCategoryResponse> getAdminCategories();

    NewsCategoryResponse createCategory(NewsCategoryRequest request);

    NewsCategoryResponse updateCategory(Integer catId, NewsCategoryRequest request);

    void deleteCategory(Integer catId);
}
