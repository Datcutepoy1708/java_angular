package com.store.service;

import com.store.dto.request.banner.CreateBannerRequest;
import com.store.dto.request.banner.UpdateBannerRequest;
import com.store.dto.response.banner.BannerResponse;
import com.store.entity.banner.BannerPosition;

import java.util.List;

public interface BannerService {

    List<BannerResponse> getPublicBanners(BannerPosition position);

    List<BannerResponse> getAdminBanners(BannerPosition position);

    BannerResponse getBannerById(Long bannerId);

    BannerResponse createBanner(CreateBannerRequest request);

    BannerResponse updateBanner(Long bannerId, UpdateBannerRequest request);

    void deleteBanner(Long bannerId);
}
