package com.store.service;

import com.store.dto.request.banner.CreateBannerRequest;
import com.store.dto.request.banner.UpdateBannerRequest;
import com.store.dto.response.banner.BannerResponse;
import com.store.entity.banner.Banner;
import com.store.entity.banner.BannerPosition;
import com.store.entity.banner.BannerStatus;
import com.store.repository.BannerRepository;
import com.store.service.impl.BannerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @InjectMocks
    private BannerServiceImpl bannerService;

    private Banner banner1;
    private Banner banner2;

    @BeforeEach
    void setUp() {
        banner1 = Banner.builder()
                .bannerId(1L)
                .title("Khuyến mãi mùa tựu trường")
                .imageUrl("/uploads/banner1.jpg")
                .linkUrl("/products")
                .position(BannerPosition.HOMEPAGE_SLIDER)
                .sortOrder(1)
                .status(BannerStatus.ACTIVE)
                .build();

        banner2 = Banner.builder()
                .bannerId(2L)
                .title("Flash Sale PC Gaming")
                .imageUrl("/uploads/banner2.jpg")
                .linkUrl("/products?category=pc-gaming")
                .position(BannerPosition.HOMEPAGE_SLIDER)
                .sortOrder(2)
                .status(BannerStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Get public active banners filters by position and date range")
    void testGetPublicBanners() {
        when(bannerRepository.findActiveBannersByPosition(eq(BannerPosition.HOMEPAGE_SLIDER), any(LocalDateTime.class)))
                .thenReturn(List.of(banner1, banner2));

        List<BannerResponse> result = bannerService.getPublicBanners(BannerPosition.HOMEPAGE_SLIDER);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Khuyến mãi mùa tựu trường");
        assertThat(result.get(1).getTitle()).isEqualTo("Flash Sale PC Gaming");
    }

    @Test
    @DisplayName("Create banner creates banner successfully")
    void testCreateBanner() {
        CreateBannerRequest request = CreateBannerRequest.builder()
                .title("Banner Mới")
                .imageUrl("/uploads/new.jpg")
                .linkUrl("/news")
                .position(BannerPosition.SIDEBAR)
                .sortOrder(3)
                .status(BannerStatus.ACTIVE)
                .build();

        when(bannerRepository.save(any(Banner.class))).thenAnswer(inv -> {
            Banner b = inv.getArgument(0);
            b.setBannerId(3L);
            return b;
        });

        BannerResponse response = bannerService.createBanner(request);

        assertThat(response).isNotNull();
        assertThat(response.getBannerId()).isEqualTo(3L);
        assertThat(response.getPosition()).isEqualTo(BannerPosition.SIDEBAR);
        verify(bannerRepository).save(any(Banner.class));
    }

    @Test
    @DisplayName("Update banner updates existing banner")
    void testUpdateBanner() {
        UpdateBannerRequest request = UpdateBannerRequest.builder()
                .title("Banner Cập Nhật")
                .imageUrl("/uploads/updated.jpg")
                .linkUrl("/updated")
                .position(BannerPosition.POPUP)
                .sortOrder(5)
                .status(BannerStatus.INACTIVE)
                .build();

        when(bannerRepository.findById(1L)).thenReturn(Optional.of(banner1));
        when(bannerRepository.save(any(Banner.class))).thenReturn(banner1);

        BannerResponse response = bannerService.updateBanner(1L, request);

        assertThat(response.getTitle()).isEqualTo("Banner Cập Nhật");
        assertThat(response.getPosition()).isEqualTo(BannerPosition.POPUP);
        assertThat(response.getStatus()).isEqualTo(BannerStatus.INACTIVE);
        verify(bannerRepository).save(banner1);
    }

    @Test
    @DisplayName("Delete banner deletes successfully")
    void testDeleteBanner() {
        when(bannerRepository.existsById(1L)).thenReturn(true);

        bannerService.deleteBanner(1L);

        verify(bannerRepository).deleteById(1L);
    }
}
