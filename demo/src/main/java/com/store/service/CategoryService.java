package com.store.service;

import com.store.dto.request.CategoryRequest;
import com.store.dto.response.CategoryResponse;
import com.store.dto.response.PageResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getCategoryTree();

    List<CategoryResponse> getRootCategories();

    List<CategoryResponse> getChildrenByParentId(Integer parentId);

    PageResponse<CategoryResponse> getCategoriesPaginated(int page, int size, String sortBy, String sortDir);

    CategoryResponse getCategoryById(Integer id);

    CategoryResponse getCategoryBySlug(String slug);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Integer id, CategoryRequest request);

    void deleteCategory(Integer id);

    long countChildren(Integer parentId);
}
