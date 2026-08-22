package com.store.service;

import com.store.dto.request.BrandRequest;
import com.store.dto.response.BrandResponse;
import com.store.dto.response.PageResponse;

import java.util.List;

public interface BrandService {

    List<BrandResponse> getAllBrands();

    PageResponse<BrandResponse> getBrandsPaginated(int page, int size, String sortBy, String sortDir);

    BrandResponse getBrandById(Integer id);

    BrandResponse getBrandBySlug(String slug);

    BrandResponse createBrand(BrandRequest request);

    BrandResponse updateBrand(Integer id, BrandRequest request);

    void deleteBrand(Integer id);
}
