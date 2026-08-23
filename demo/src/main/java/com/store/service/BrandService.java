package com.store.service;

import com.store.dto.request.BrandRequest;
import com.store.dto.request.BulkActionRequest;
import com.store.dto.response.BrandResponse;
import com.store.dto.response.BulkActionResult;
import com.store.dto.response.PageResponse;

import java.util.List;

public interface BrandService {

    List<BrandResponse> getAllBrands();

    PageResponse<BrandResponse> getBrandsPaginated(int page, int size, String keyword, String status, String sortBy, String sortDir);

    PageResponse<BrandResponse> getDeletedBrands(int page, int size);

    BrandResponse getBrandById(Integer id);

    BrandResponse getBrandBySlug(String slug);

    BrandResponse createBrand(BrandRequest request);

    BrandResponse updateBrand(Integer id, BrandRequest request);

    /** Soft-delete: sets deleted_at = NOW(), does NOT change status */
    void softDeleteBrand(Integer id);

    /** Restore: sets deleted_at = NULL, does NOT change status */
    void restoreBrand(Integer id);

    BulkActionResult bulkAction(BulkActionRequest request);

    /** @deprecated Use softDeleteBrand instead */
    @Deprecated
    void deleteBrand(Integer id);
}
