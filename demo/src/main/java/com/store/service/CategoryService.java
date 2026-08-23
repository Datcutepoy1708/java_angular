package com.store.service;

import com.store.dto.request.BulkActionRequest;
import com.store.dto.request.CategoryRequest;
import com.store.dto.response.BulkActionResult;
import com.store.dto.response.CategoryResponse;
import com.store.dto.response.PageResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getCategoryTree();

    List<CategoryResponse> getRootCategories();

    List<CategoryResponse> getChildrenByParentId(Integer parentId);

    PageResponse<CategoryResponse> getCategoriesPaginated(int page, int size, String keyword, String sortBy, String sortDir);

    PageResponse<CategoryResponse> getDeletedCategories(int page, int size);

    CategoryResponse getCategoryById(Integer id);

    CategoryResponse getCategoryBySlug(String slug);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Integer id, CategoryRequest request);

    /** Soft-delete category and all descendants recursively */
    void softDeleteCategory(Integer id);

    /** Restore category and all descendants that are currently deleted */
    void restoreCategory(Integer id);

    BulkActionResult bulkAction(BulkActionRequest request);

    long countChildren(Integer parentId);

    /** @deprecated Use softDeleteCategory instead */
    @Deprecated
    void deleteCategory(Integer id);
}
